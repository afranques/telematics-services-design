import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class cliente extends Thread{ //ESTE HILO ES EL MAIN, ENCARGADO DE LLAMAR A LOS DEMÁS HILOS Y DE GESTIONAR EL ESTABLECIMIENTO DE LA CONEXIÓN
    static boolean paradaout=false; //defino unas variables globales que podrán ser modificadas por las otras clases para que así puedan comunicarse entre ellas.
    static boolean paradain=false;
    static boolean ficherobusy=false;
    static boolean ficherostop=false;
    static boolean soyreceptor=false;
    static boolean iniciotransf=false;
    static boolean transfdenied=false;
    static boolean redirigeteclado=false;
    static String nombredestino="";
    static boolean finout=false;
    public static void main(String[] args) throws Exception {
        Socket s;
        PrintWriter out; //buffer usado en los tres hilos (flujoOut, flujoIn, ficheroOut) para el envío de mensajes de texto
        BufferedReader in; //buffer usado en flujoIn para la recepción de mensajes de texto (es distinto del que cada hilo pueda usar para leer de teclado)
        try {
            //InetAddress ipLocal=InetAddress.getLocalHost(); //forma automática de obtener la IP local (ya que nuestra conexión va de mi ordenador hacia mi ordenador)
            //s = new Socket(ipLocal,8081); //aunque he llamado "ipLocal" a la variable cabe destacar que el parámetro se refiere a la IP y puerto de destino
            BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in)); //buffer usado para la lectura de teclado
            System.out.println("Introducir dirección IP:");
            InetAddress ip = InetAddress.getByName(teclado.readLine());
            teclado.close();
            s = new Socket(ip,8081); //IP y puerto destino
            out = new PrintWriter(new OutputStreamWriter(s.getOutputStream())); //el buffer de salida apunta a través del socket (conexión TCP creada) al servidor.
            in = new BufferedReader(new InputStreamReader(s.getInputStream())); //el buffer de entrada apunta a través del socket al buffer de salida del servidor.

            System.out.println("Has iniciado un chat con "+ip+":");
            System.out.println("(para enviar un archivo escriba @rchivo)");
            System.out.println("(para terminar escriba fin)");

            flujoOut a = new flujoOut(s,out); //declaro el hilo de envío de mensajes de texto
            flujoIn b = new flujoIn(s,in,out); //declaro el hilo de recepción de mensajes de texto
            a.start(); //inicio ambos hilos
            b.start();

            //no compruebo si el hilo ficheroOut está cerrado porque el hecho de que los otros dos lo estén ya implicará que éste también lo está
            while(!paradain || !paradaout) { //no cierres el socket mientras por lo menos un hilo siga abierto
                sleep(1000); //compruébalo cada 1000 milisegundos (1 segundo)
            }

            out.close(); //cierro el buffer de salida
            in.close(); //cierro el buffer de entrada
            s.close(); //cierro el socket (conexión TCP creada entre cliente y servidor)
            System.out.println("Aplicacion cerrada.");
        }
        catch (IOException e) {
            System.out.println("SE HA PRODUCIDO UN ERROR EN LA APLICACION");
        }
    }
}