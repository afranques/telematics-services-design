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
                texto = in.readLine(); //guardamos en un string el texto que vamos recibiendo de la otra máquina
                if(texto!=null) {
                    if(texto.startsWith("@rchivo")) {
                        if(texto.startsWith("@rchivotrx")) {
                            escritura.println(texto.substring(10)); //como texto contiene el comando de control mas el texto a recibir debo empezar con offset
                        }
                        else if(texto.equals("@rchivofin")) {
                            System.out.println("Transferencia terminada.");
                            escritura.close();
                            bw.close();
                            servidor.ficherobusy=false;
                            servidor.soyreceptor=false;
                        }
                        else if(texto.startsWith("@rchivoini")) {
                            if(texto.equals("@rchivoiniok")) { //si lo que recibo es una confirmación a una petición para enviar desde este usuario
                                servidor.iniciotransf=true;
                                System.out.println("Transferencia iniciada.");
                            }
                            else if(texto.equals("@rchivoinidenied")) { //si han denegado mi petición de enviar fichero
                                servidor.iniciotransf=true;
                                servidor.transfdenied=true;
                            }
                            else { //o si por el contrario es el servidor quien me ha hecho la petición de envío a mi con un @rchivoininombreorigen
                                //AQUI DEBERA PEDIR CON QUE NOMBRE QUIERO GUARDAR EL ARCHIVO
                                System.out.println("El otro usuario le envia un archivo llamado "+texto.substring(10));
                                System.out.println("¿Con que nombre quiere guardarlo? (por ejemplo prueba.txt, o pulse CANCELAR para denegar la operacion): ");
                                servidor.redirigeteclado=true; //le indico al flujoOut que rediriga el texto de teclado hacia este flujo (con variables estáticas)
                                while(servidor.redirigeteclado) { //mientras el otro hilo no haya terminado con el proceso de preguntar al usuario
                                    sleep(1000);
                                }
                                if(servidor.transfdenied) {
                                    out.println("@rchivoinidenied");
                                    out.flush();
                                    servidor.transfdenied=false; //pongo la bandera a su estado inicial preparada para otra transferencia
                                }
                                else {
                                    bw = new BufferedWriter(new FileWriter(servidor.nombredestino)); //creo el archivo y lo dejo preparado para ser escrito
                                    escritura = new PrintWriter(bw); //creo el buffer que irá escribiendo sobre el archivo
                                    out.println("@rchivoiniok"); //indico al otro usuario que empiece la transferencia
                                    out.flush();
                                    servidor.ficherobusy=true;
                                    servidor.soyreceptor=true;
                                    System.out.println("Transferencia iniciada.");
                                }
                            }
                        }
                        else {
                            System.out.println("Comando de recepcion de archivo no reconocido.");
                            System.out.println("Por favor, contacte con nuestros tecnicos para que resuelvan el problema.");
                        }
                    }
                    else if(texto.equals("finaccepted")) {
                        bandera=false; //si recibo "finaccepted" es que ya no voy a recibir nada más así que el hilo flujoIn queda cerrado
                    }
                    else if(texto.equals("finrequest")) {
                        if(servidor.ficherobusy) { //si recibo un finrequest y hay una transferencia en curso es que yo soy el emisor y debo detenerla
                            servidor.ficherostop=true;
                            while(servidor.ficherobusy) { //aunque he indicado el cierre forzado de la transferencia me espero a que se cierre el hilo
                                sleep(1000);
                            }
                        }
                        bandera=false; //si recibo "finrequest" es que ya no voy a recibir nada más así que el hilo flujoIn queda cerrado
                        servidor.finout = true; //ordeno empezar el cierre de flujoOut
                        System.out.println("El otro usuario ha cerrado la conexion, ya no recibira tus mensajes.");
                        //ANTES DE MANDAR EL FINACCEPTED ASEGURATE DE QUE EL FLUJO DE ARCHIVO OUT TAMPOCO VA A MANDAR NADA MÁS, YA SEA PORQUE SE HA CANCELADO O PORQUE HA TERMINADO
                        //PARA ELLO DECLARA UNA VARIABLE ESTATICA QUE SE PONGA A TRUE CUANDO LLEGA A LA ULTIMA LINEA DE DICHO FLUJO
                        out.println("finaccepted"); //este mensaje debo mandarlo desde este hilo porque el otro está parado en el readLine()
                        out.flush();
                        //con finaccepted yo le estoy indicando que ya no voy a mandar nada más
                    }
                    else {
                        System.out.println("El otro usuario dice: "+texto); //y lo voy mostrando por pantalla
                    }
                }
            }
            servidor.paradain = true; //hilo entrada cerrado
            //PERO TIENES QUE HACER ALGUNA FORMA PARA QUE LUEGO SE PUDIERA MANDAR OTRO ARCHIVO SI SE QUISIERA
        }
        catch (Exception e) {
            System.out.println("SE HA PRODUCIDO UN ERROR EN LA RECEPCION DE UN MENSAJE");
        }
    }
}