/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controlador;

//import static controlador.RecibirHilo.entrada;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import static java.lang.Thread.sleep;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 *
 * @author David
 */
public class EnviarHilo implements Runnable {

    //static Socket socket;
    //static DataInputStream entrada;
    //static DataOutputStream salida;
    private int error = -1;
    private String control = "";
    private int respuesta = -1;

    public void transporte(ArrayList<String> textoEntramado, int error, int perdida) throws InterruptedException, IOException {
        int randomNum = ThreadLocalRandom.current().nextInt(0, textoEntramado.size());
        this.error = error;
        int controlTotal = 0;
        
         
        
            Socket socket = new Socket("127.0.0.1", 3000);
            DataInputStream entrada = new DataInputStream(socket.getInputStream());
            DataOutputStream salida = new DataOutputStream(socket.getOutputStream());
        
            //Logger.getLogger(EnviarHilo.class.getName()).log(Level.SEVERE, null, ex);
        

            

        try {
//            System.out.print(socket.getPort());
            

            for (int contadorTramas = 0; contadorTramas < textoEntramado.size(); contadorTramas++) {
                //socket.setSoTimeout(0);
                System.out.println("Iteracion:" + contadorTramas);
                String men = textoEntramado.get(contadorTramas);
                if (randomNum == contadorTramas && error == 1 && perdida == 0) {

                    for (int car = 0; car < 13; car++) {
                        salida.writeChar(men.charAt(car));
                        //sleep(500); 
                    }
                    if (men.charAt(13) == '0') {
                        salida.writeChar('1');
                        //sleep(500); 
                    } else {
                        salida.writeChar('0');
                        //sleep(500); 
                    }

                    for (int car = 14; car < men.length(); car++) {
                        salida.writeChar(men.charAt(car));
                        //sleep(500); 
                    }
                    error = 0;
                } else if (randomNum == contadorTramas && error == 0 && perdida == 1) {

                } else {
                    for (Character car : men.toCharArray()) {
                        salida.writeChar(car);
                        //sleep(500);
                    }
                }

                //int controlRet = 0;
                //long startTime = System.currentTimeMillis();
                //while(controlRet!=1 ||(System.currentTimeMillis()<startTime+5000)) {
                //sleep(1);
                //Integer i = 0;
                //while (i++ < 50) {
                //Thread.sleep(100);
                try {
                    socket.setSoTimeout(5000);
                    respuesta = entrada.readInt();
                } catch (SocketTimeoutException e) {
                    socket.setSoTimeout(0);
                    continue;
                }

                //controlRet=1;
                System.out.println("Dentro while");
                //break;

                // }
                //  controlRet=1;
                System.out.println("sale while");

                if (respuesta == 0 || respuesta == -1) {
                    controlTotal++;
                    contadorTramas--;

                } else if (respuesta == 1) {
                    controlTotal = 0;

                }
                System.out.println("Respuesta=" + respuesta);
                if (controlTotal == 5) {
                    respuesta = -1;
                    break;
                }
                respuesta = -1;

            }
            salida.writeChar('a');

            entrada.close();
            salida.close();
            socket.close();

        } catch (SocketException ex) {

            Logger.getLogger(controlador.class.getName()).log(Level.SEVERE, null, ex);
            try {
                entrada.close();
                salida.close();
                socket.close();
            } catch (IOException ex1) {
                Logger.getLogger(EnviarHilo.class.getName()).log(Level.SEVERE, null, ex1);
            }
        } catch (IOException ex) {
            Logger.getLogger(EnviarHilo.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public void run() {

    }

}
