import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class flujoIn extends Thread { //ESTE HILO SERÁ EL ENCARGADO DE RECIBIR LOS MENSAJES DE TEXTO
    Socket s; //declaro unas variables del mismo tipo de las que me llegarán como parámetros del constructor.
    BufferedReader in;
    PrintWriter out; //deberé usar el buffer de salida para mandar el finaccepted como respuesta a un finrequest
    flujoIn(Socket s,BufferedReader in,PrintWriter out) { //constructor de flujoIn con los parámetros que llegan desde el main
        this.s = s; //asigno el socket que me llega como parámetro con el que acabo de declarar
        this.in = in; //asigno el buffer de entrada que me llega como parámetro con el que acabo de declarar
        this.out = out;
    }

    public void run() { //redefino el método run para indicar qué hara cuando se llame el hilo flujoIn
        try {
            String texto;
            BufferedWriter bw=null;
            PrintWriter escritura=null;
            boolean bandera=true;
            while(bandera) { //mientras nadie ordene detener la comunicación...
                texto = in.readLine(); //guardamos en un string el texto que vamos recibiendo de la otra máquina (hasta que no reciba algo no pasará de esta línea)
                if(texto!=null) { //si el texto recibido no es null
                    if(texto.startsWith("@rchivo")) { //si el texto recibido lleva la "cabecera" @rchivo
                        if(texto.startsWith("@rchivotrx")) {
                            escritura.println(texto.substring(10)); //como el texto recibido contiene la cabecera más el texto de interés debo empezar con offset, luego lo guardo en el archivo
                        }
                        else if(texto.equals("@rchivofin")) { //si recibo @rvhivofin es que el otro usuario ha terminado con el envío por lo que cierro el buffer de escritura
                            System.out.println("Transferencia terminada.");
                            escritura.close();
                            bw.close();
                            cliente.ficherobusy=false; //indico que ya no hay una transferencia en curso
                            cliente.soyreceptor=false; //indico que ya no soy receptor
                        }
                        else if(texto.startsWith("@rchivoini")) { //si recibo la secuencia de inicio de transferencia
                            if(texto.equals("@rchivoiniok")) { //si lo que recibo es una confirmación a la petición que le he hecho yo desde el hilo ficheroOut
                                cliente.iniciotransf=true; //le indico a dicho hilo que ya puede empezar a enviarle el fichero
                                System.out.println("Transferencia iniciada.");
                            }
                            else if(texto.equals("@rchivoinidenied")) { //si han denegado mi petición de enviar fichero
                                cliente.iniciotransf=true; //indico al hilo que ya puede seguir con el hilo pero que no llegue a enviar nada porque me lo han denegado
                                cliente.transfdenied=true;
                            }
                            else { //o si por el contrario es el servidor quien me ha hecho la petición de envío a mi con un @rchivoininombreorigen
                                //le pido al usuario con qué nombre quiere guardar el archivo que están a punto de enviarme
                                System.out.println("El otro usuario le envia un archivo llamado "+texto.substring(10));
                                System.out.println("¿Con que nombre quiere guardarlo? (por ejemplo prueba.txt, o pulse CANCELAR para denegar la operacion): ");
                                cliente.redirigeteclado=true; //le indico al flujoOut que rediriga el texto de teclado hacia este flujo (con variables estáticas)
                                while(cliente.redirigeteclado) { //mientras el otro hilo no haya terminado con el proceso de preguntar al usuario
                                    sleep(1000);
                                }
                                if(cliente.transfdenied) { //si el usuario ha indicado que rechaza la petición de envío le indico al otro usuario que le rechazo su oferta
                                    out.println("@rchivoinidenied");
                                    out.flush();
                                    cliente.transfdenied=false; //pongo la bandera a su estado inicial preparada para otra transferencia
                                }
                                else { //en caso de que haya aceptado la oferta de transferencia inicio los buffers de escritura de archivo con el nombre que el usuario ha introducido
                                    bw = new BufferedWriter(new FileWriter(cliente.nombredestino)); //creo el archivo y lo dejo preparado para ser escrito
                                    escritura = new PrintWriter(bw); //creo el buffer que irá escribiendo sobre el archivo
                                    out.println("@rchivoiniok"); //indico al otro usuario que empiece la transferencia
                                    out.flush();
                                    cliente.ficherobusy=true; //indico que a partir de ahora hay una transferencia en curso
                                    cliente.soyreceptor=true; //indico que estoy actuando como receptor
                                    System.out.println("Transferencia iniciada.");
                                }
                            }
                        }
                        else { //si el otro usuario me envía algo con la cabecera del tipo @rchivo pero no he reconocido dicho comando
                            System.out.println("Comando de transferencia de archivo no reconocido.");
                            System.out.println("Por favor, contacte con nuestros tecnicos para que resuelvan el problema.");
                        }
                    }
                    else if(texto.equals("finaccepted")) { //si en vez de algo de archivo he recibido una confirmación a una petición de cierre que yo le he hecho al otro usuario
                        bandera=false; //si recibo "finaccepted" es que ya no voy a recibir nada más así que indico el cierre de este flujo
                    }
                    else if(texto.equals("finrequest")) { //si he recibido una petición de cierre
                        if(cliente.ficherobusy) { //y hay una transferencia en curso es que yo soy el emisor y debo detenerla
                            cliente.ficherostop=true;
                            while(cliente.ficherobusy) { //aunque he indicado el cierre forzado de la transferencia me espero a que se cierre el hilo
                                sleep(1000);
                            }
                        }
                        bandera=false; //si recibo "finrequest" es que ya no voy a recibir nada más así que indico el cierre de este hilo
                        cliente.finout = true; //indico que el cierre ha sido impuesto por el otro usuario (no voluntario)
                        System.out.println("El otro usuario ha cerrado la conexion, ya no recibira tus mensajes");
                        out.println("finaccepted"); //este mensaje debo mandarlo desde este hilo porque el otro está parado en el readLine()
                        out.flush();
                        //con finaccepted yo le estoy indicando que ya no voy a mandar nada más
                    }
                    else { //si el texto recibido no es null, ni lleva la cabecera de transferencia de archivo ni es un mensaje de cierre
                        System.out.println("El otro usuario dice: "+texto); //lo voy mostrando por pantalla
                    }
                }
            }
            cliente.paradain = true; //hilo entrada cerrado
        }
        catch (Exception e) {
            System.out.println("SE HA PRODUCIDO UN ERROR EN LA RECEPCION DE UN MENSAJE");
        }
    }
}