import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class servidor extends Thread{ //ESTE HILO ES EL MAIN, ENCARGADO DE LLAMAR A LOS DEMÁS HILOS Y DE GESTIONAR EL ESTABLECIMIENTO DE LA CONEXIÓN
    static boolean paradaout=false; //defino unas variables globales que podrán ser modificadas por otros métodos para hacerle saber al main cuando han terminado los otros hilos
    static boolean paradain=false;
    static boolean ficherobusy=false;
    static boolean ficherostop=false;
    static boolean soyreceptor=false;
    static boolean iniciotransf=false;
    static boolean transfdenied=false;
    static boolean redirigeteclado=false;
    static String nombredestino="";
    static boolean finout=false;
    public static void main(String args[]) throws Exception {
        Socket s;
        ServerSocket serv;
        PrintWriter out;
        BufferedReader in; //buffer usado en flujoIn para la recepción de mensajes de texto (es distinto del que tiene la clase flujoOut para leer de teclado)
        try {
            serv = new ServerSocket(8081); //apertura del puerto (pero la conexión todavía no está creada)
            s = serv.accept(); //establece la conexión TCP pasivamente cuando el servidor la solicita
            out = new PrintWriter(new OutputStreamWriter(s.getOutputStream())); //el buffer de salida apunta a través del socket (conexión TCP creada) al servidor.
            in = new BufferedReader(new InputStreamReader(s.getInputStream())); //el buffer de entrada apunta a través del socket al buffer de salida del servidor.

            System.out.println("Has iniciado un chat con otro usuario:");
            System.out.println("(para enviar un archivo escriba @rchivo)");
            System.out.println("(para terminar escriba fin)");

            flujoOut a = new flujoOut(s,out); //declaro el hilo de envío de mensajes de texto
            flujoIn b = new flujoIn(s,in,out); //declaro el hilo de recepción de mensajes de texto
            a.start(); //inicio ambos hilos
            b.start();

            while(!paradain || !paradaout) { //no cierres el socket mientras por lo menos un hilo siga abierto
                sleep(1000); //cada 1 segundo compruebo si los otros hilos han terminado
            }

            out.close(); //cierro el buffer de salida
            in.close(); //cierro el buffer de entrada
            s.close(); //cierro el socket (conexión TCP creada entre servidor y servidor)
            serv.close(); //¿este hace falta?
            System.out.println("Aplicacion cerrada.");
        }
        catch (IOException e) {
            System.out.println("SE HA PRODUCIDO UN ERROR EN LA APLICACION");
        }
    }
}