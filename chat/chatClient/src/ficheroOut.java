import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ficheroOut extends Thread { //ESTE HILO SERÁ EL ENCARGADO DE ENVIAR ARCHIVOS
    Socket s; //declaro unas variables del mismo tipo de las que me llegarán como parámetros del constructor.
    PrintWriter out;
    String nombreorigen;
    ficheroOut(Socket s,PrintWriter out,String nombreorigen) { //constructor de archivoOut con los parámetros que llegan desde flujoOut, que a su vez llegan desde el main
        this.s = s; //asigno el socket que me llega como parámetro con el que acabo de declarar
        this.out = out; //asigno el buffer de salida que me llega como parámetro con el que acabo de declarar
        this.nombreorigen = nombreorigen;
    }

    public void run() { //redefino el método run para indicar qué hara cuando se llame el hilo ficheroOut
        try {
            BufferedReader br = new BufferedReader(new FileReader(nombreorigen)); //abro el archivo y lo dejo preparado para ser leído
            out.println("@rchivoini"+nombreorigen); //envío el comando de control de inicio de transferencia al otro usuario
            out.flush();
            //MIENTRAS EL HILO FLUJOIN NO ME CONFIRME QUE EL OTRO USUARIO ESTÁ PREPARADO PARA RECIBIR EL ARCHIVO HAZ UN WHILE INFINITO CON SLEEP (PARA QUE NO CONSUMA TANTO)
            //SI SALGO DE ESE BUCLE ES QUE EL OTRO USUARIO YA ESTÁ PREPARADO PARA RECIBIR Y POR TANTO EMPIEZO LA TRANSFERENCIA
            while(!cliente.iniciotransf) {
                sleep(1000);
            }
            cliente.iniciotransf=false; //pongo esta bandera preparada para la siguiente transferencia
            if(cliente.transfdenied) {
                System.out.println("El otro usuario ha rechazado tu archivo.");
                cliente.transfdenied=false; //pongo esta bandera preparada para la siguiente transferencia
            }
            else {
                String texto;
                boolean bandera=true;
                while(bandera && !cliente.ficherostop) { //mientras no termine la transferencia o se cancele...
                    texto=br.readLine();
                    if(texto==null) { //cuando he alcanzado la última línea a enviar
                        out.println("@rchivofin"); //indico que ha terminado la transferencia del archivo
                        out.flush(); //inmediatamente
                        bandera=false;
                        System.out.println("Transferencia terminada.");
                    }
                    else {
                        out.println("@rchivotrx"+texto); //a través del buffer de salida voy enviando el archivo al buffer de entrada del otro usuario
                        out.flush(); //inmediatamente
                    }
                }
                if(cliente.ficherostop) { //si desde los otros hilos (ya sea el de emisión o el de recepción) se recibe la orden de detener el programa
                    System.out.println("Se ha interrumpido la transferencia por peticion de cierre.");
                    out.println("@rchivotrxSe ha interrumpido la transferencia por peticion de cierre.");
                    out.println("@rchivofin");
                    out.flush();
                    cliente.ficherostop=false; //vuelvo a poner a esta bandera preparada para la siguiente transferencia
                }
            }
            br.close();
        }
        catch (FileNotFoundException e){
            System.out.println("Fichero no encontrado.");
        }
        catch (Exception e) {
            System.out.println("SE HA PRODUCIDO UN ERROR AL ENVIAR EL FICHERO");
        }
        cliente.ficherobusy=false;
    }
}
