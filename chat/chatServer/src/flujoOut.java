import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class flujoOut extends Thread { //ESTE HILO SERÁ EL ENCARGADO DE ENVIAR LOS MENSAJES DE TEXTO
    Socket s; //declaro unas variables del mismo tipo de las que me llegarán como parámetros del constructor.
    PrintWriter out;
    flujoOut(Socket s, PrintWriter out) { //constructor de flujoOut con los parámetros que llegan desde el main
        this.s = s; //asigno el socket que me llega como parámetro con el que acabo de declarar
        this.out = out; //asigno el buffer de salida que me llega como parámetro con el que acabo de declarar
    }

    public void run() { //redefino el método run para indicar qué hara cuando se llame el hilo flujoOut
        try {
            BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in)); //buffer usado para la lectura de teclado
            boolean bandera=true;
            while(bandera && !servidor.finout) { //mientras desde este flujo no se indique terminar ni el otro usuario solicite el cierre...
                String texto = teclado.readLine(); //me quedo esperando a que este usuario introduzca texto (hasta que no introduzca texto no sigo)
                if(servidor.finout) { //si he recibido un finrequest mientras yo estaba escribiendo ya no lo envío
                    System.out.println("Su mensaje no ha podido ser enviado, el otro usuario cerro la conexión.");
                }
                else {
                    if(texto==null) {
                        System.out.println("No he sabido interpretar el texto introducido.");
                    }
                    else if(servidor.redirigeteclado) { //si desde flujoIn se está tramitando la respuesta a una propuesta de transferencia...
                        if(texto.equals("CANCELAR")) {
                            servidor.transfdenied=true;
                        }
                        else {
                            String aux=texto;
                            do {
                                servidor.nombredestino=aux;
                                System.out.println("Ha indicado que desea guardar el archivo como "+aux);
                                System.out.println("Si esta de acuerdo introduzca OK, de lo contrario introduzca el nuevo nombre.");
                                aux=teclado.readLine();
                            }while(!aux.equals("OK"));
                        }
                        servidor.redirigeteclado=false; //devuelvo el turno a flujoIn y este flujo vuelve a la normalidad
                    }
                    else if(texto.equals("@rchivo")) {
                        if(servidor.ficherobusy) {
                            System.out.println("Ya esta enviando un archivo en estos momentos, por favor espere a completar la transferencia actual para empezar otra.");
                        }
                        else {
                            System.out.println("Introduzca nombre del archivo a enviar (por ejemplo prueba.txt) o CANCELAR para cancelar la operacion: ");
                            String nombreorigen = teclado.readLine();
                            if(!nombreorigen.equals("CANCELAR")) {
                                servidor.ficherobusy=true; //indico que el proceso de transferencia está ocupado ¿NO PUEDES USAR LA YA EXISTENTE servidor.PARADAFICHERO?
                                ficheroOut c = new ficheroOut(s,out,nombreorigen); //declaro el hilo de recepción de mensajes de texto
                                c.start();
                            }
                        }
                    }
                    else {
                        if(texto.equals("fin")) {
                            //aqui deberas poner algo para asegurarte que el flujoarchivo esta terminado tambien, y sino esperarte hasta que lo esté o preguntar si cerrar.
                            //NO MANDES EL FINREQUEST HASTA QUE YA NO VAYAS A MANDAR NADA MÁS YA QUE SI MANDAS FINREQUEST EL OTRO ENTENDERÁ QUE YA NO MANDAS NADA MAS
                            if(servidor.ficherobusy) {
                                System.out.println("Hay una transferencia en curso, ¿desea cerrar de todos modos? (SI/NO): ");
                                String aux;
                                boolean textocorrecto=false;
                                do {
                                    aux=teclado.readLine();
                                    if(aux.equals("SI")) {
                                        if(!servidor.soyreceptor) { //si no soy el receptor del archivo (soy emisor)
                                            servidor.ficherostop=true;
                                            while(servidor.ficherobusy) { //aunque he indicado el cierre forzado de la transferencia me espero a que se cierre el hilo
                                                sleep(1000);
                                            }
                                        }
                                        out.println("finrequest");
                                        out.flush();
                                        teclado.close();
                                        bandera=false; //ya no voy a enviar nada más, así que el hilo flujoOut queda cerrado y el buffer de teclado también
                                        textocorrecto=true;
                                    }
                                    else if(aux.equals("NO")) {
                                        textocorrecto=true;
                                    }
                                    else {
                                        System.out.println("Respuesta no reconocida, introduzca SI o NO");
                                    }
                                }while(!textocorrecto);
                            }
                            else {
                                out.println("finrequest");
                                out.flush();
                                teclado.close();
                                bandera=false; //ya no voy a enviar nada más, así que el hilo flujoOut queda cerrado y el buffer de teclado también
                            }
                        }
                        else {
                            out.println(texto); //redirijo y envío ese string hacia el buffer de salida, el cual apunta a través del socket al buffer de entrada del servidor.
                            out.flush(); //indico que idependientemente de que haya pocos datos como para crear un paquete lo mande inmediatamente.
                        }
                    }
                }
            }
            if(servidor.finout) { //si el cierre ha sido impuesto por un finrequest deberé cerrar el buffer teclado aqui ya que la ultima iteracion no ha pasado por el bucle
                teclado.close(); //debo cerrar el buffer de teclado aquí porque no ha ejecutado el cierre que hay dentro el bucle, es un cierre impuesto, no voluntario.
            }
            servidor.paradaout = true; //hilo cerrado.
        }
        catch (Exception e) {
            System.out.println("SE HA PRODUCIDO UN ERROR EN EL ENVIO DE SU MENSAJE");
        }
    }
}