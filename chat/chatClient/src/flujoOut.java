import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class flujoOut extends Thread { //ESTE HILO SERÁ EL ENCARGADO DE ENVIAR LOS MENSAJES DE TEXTO
    Socket s; //declaro unas variables del mismo tipo de las que me llegarán como parámetros del constructor.
    PrintWriter out;
    flujoOut(Socket s, PrintWriter out) { //constructor de flujoOut con los parámetros que llegan desde el main
        this.s = s; //asigno el socket que me llega con el que acabo de declarar
        this.out = out; //asigno el buffer de salida que me llega con el que acabo de declarar
    }

    public void run() { //redefino el método run para indicar qué hará cuando se llame el hilo flujoOut
        try {
            BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in)); //buffer usado para la lectura de teclado
            boolean bandera=true;
            while(bandera && !cliente.finout) { //mientras desde este flujo no se indique terminar ni el otro usuario solicite el cierre...
                String texto = teclado.readLine(); //me quedo esperando a que este usuario introduzca texto (hasta que no introduzca texto no sigo)
                if(cliente.finout) { //si he recibido un finrequest mientras yo estaba escribiendo ya no lo envío
                    System.out.println("Su mensaje no ha podido ser enviado, el otro usuario cerro la conexión.");
                }
                else { //si nadie ha ordenado terminar
                    if(texto==null) {
                        System.out.println("No he sabido interpretar el texto introducido.");
                    }
                    else if(cliente.redirigeteclado) { //este bloque sucede cuando he recibido una petición de transferencia de archivo y debo tratarla
                        if(texto.equals("CANCELAR")) { //si el usuario receptor rechaza la petición de transferencia
                            cliente.transfdenied=true;
                        }
                        else { //en caso que no la haya rechazado ¿con qué nombre quiere guardar el archivo a recibir?
                            String aux=texto;
                            do {
                                cliente.nombredestino=aux;
                                System.out.println("Ha indicado que desea guardar el archivo como "+aux);
                                System.out.println("Si esta de acuerdo introduzca OK, de lo contrario introduzca el nuevo nombre.");
                                aux=teclado.readLine();
                            }while(!aux.equals("OK")); //este OK deberá estar en mayúsculas ya que el equals es sensible a éstas.
                        }
                        cliente.redirigeteclado=false; //devuelvo el turno a flujoIn y este flujo vuelve a la normalidad
                    }
                    else if(texto.equals("@rchivo")) { //si el usuario ha introducido el comando de inicio de transferencia
                        if(cliente.ficherobusy) { //si hay una transferencia en curso
                            System.out.println("Ya esta enviando un archivo en estos momentos, por favor espere a completar la transferencia actual para empezar otra.");
                        }
                        else {
                            System.out.println("Introduzca nombre del archivo a enviar (por ejemplo prueba.txt) o CANCELAR para cancelar la operacion: ");
                            String nombreorigen = teclado.readLine();
                            if(!nombreorigen.equals("CANCELAR")) {
                                cliente.ficherobusy=true; //indico que el proceso de transferencia está ocupado a partir de ahora
                                ficheroOut c = new ficheroOut(s,out,nombreorigen); //declaro el hilo de emisión de archivo
                                c.start(); //y lo arranco
                            }
                        }
                    }
                    else {
                        if(texto.equals("fin")) { //si el usuario indica que quiere cerrar el programa se inicia el siguiente bloque
                            //aquí me aseguro que el flujo de archivo ha terminado bien o si hay que esperar hasta que lo esté o preguntar si desea cerrarlo.
                            //NO MANDO EL FINREQUEST HASTA QUE YA NO VAYA A ENVIAR NADA MÁS, YA QUE SI MANDO FINREQUEST EL OTRO ENTENDERÁ QUE YA NO ENVÍO NADA MAS
                            if(cliente.ficherobusy) { //si hay una transferencia en curso
                                System.out.println("Hay una transferencia en curso, ¿desea cerrar de todos modos? (SI/NO): ");
                                String aux;
                                boolean textocorrecto=false;
                                do { //este bloque se encarga de restringir que el usuario tan sólo pueda escribir SI o NO
                                    aux=teclado.readLine();
                                    if(aux.equals("SI")) {
                                        if(!cliente.soyreceptor) { //si no soy el receptor del archivo (soy emisor)
                                            cliente.ficherostop=true; //indico al hilo de envío de fichero que debe detenerse
                                            while(cliente.ficherobusy) { //aunque he indicado el cierre forzado de la transferencia me espero a que se cierre el hilo
                                                sleep(1000);
                                            }
                                        }
                                        out.println("finrequest"); //una vez me he asegurado que el hilo de transferencia está cerrado inicio la negociación de cierre normal
                                        out.flush();
                                        teclado.close();
                                        bandera=false; //ya no voy a enviar nada más, así que el hilo flujoOut queda cerrado y el buffer de teclado también
                                        textocorrecto=true; //para que ya no vuelva a preguntar más si SI o NO
                                    }
                                    else if(aux.equals("NO")) {
                                        textocorrecto=true; //si indica que no quiere detener la transferencia no hago nada, tan sólo para de preguntar si SI o NO
                                    }
                                    else {
                                        System.out.println("Respuesta no reconocida, introduzca SI o NO");
                                    }
                                }while(!textocorrecto);
                            }
                            else { //si no hay ninguna transferencia en curso
                                out.println("finrequest");
                                out.flush();
                                teclado.close();
                                bandera=false; //ya no voy a enviar nada más, así que el hilo flujoOut queda cerrado y el buffer de teclado también
                            }
                        }
                        else { //si no se ha introducido null, ni se está tramitando el inicio de recepción de archivo, ni se está tramitando el inicio de envío de archivo, ni ha introducido fin
                            out.println(texto); //redirijo y envío ese string hacia el buffer de salida, el cual apunta a través del socket al buffer de entrada del otro usuario.
                            out.flush(); //indico que idependientemente de que haya pocos datos como para crear un paquete lo mande inmediatamente.
                        }
                    }
                }
            }
            if(cliente.finout) { //si el cierre ha sido impuesto por un finrequest que ha enviado el otro usuario deberé cerrar el buffer teclado aqui ya que la ultima iteracion no ha pasado por el bucle
                teclado.close(); //debo cerrar el buffer de teclado aquí porque no ha ejecutado el cierre que hay dentro el bucle, es un cierre impuesto por el otro usuario, no voluntario.
            }
            cliente.paradaout = true; //hilo cerrado.
        }
        catch (Exception e) {
            System.out.println(e.toString());
            System.out.println("SE HA PRODUCIDO UN ERROR EN EL ENVIO DE SU MENSAJE");
        }
    }
}

