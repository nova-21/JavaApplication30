/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controlador;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import static java.lang.Thread.sleep;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;
import operaciones.Conversor;
import vistas.Vista;

/**
 *
 * @author David
 */
public class controlador implements ActionListener {

    private Vista view;
    private int error = 0;
    private int perdida = 0;
    private int DELAY = 1;
    
    private FileManager fileManager;

    private Conversor convertir = new Conversor();
    private ArrayList<String> textoBinario = new ArrayList<String>();
    private ArrayList<String> textoRedundancia = new ArrayList<String>();
    private ArrayList<String> textoEntramado = new ArrayList<String>();
    InputStream entrada;
    OutputStream salida;
    String path = "";

    public controlador(Vista view) {

        this.view = view;
        this.fileManager = FileManager.getObject(view);
        this.fileManager.cargarArchivosRecibidos();
        this.view.btnEnviar.addActionListener(this);
        this.view.btnLimpiar.addActionListener(this);
        this.view.ckCRC.addActionListener(this);
        this.view.ckHamm.addActionListener(this);
        this.view.btnArchivo.addActionListener(this);
        this.view.btnRecibir.addActionListener(this);
        this.view.ckArchivo.addActionListener(this);
        this.view.btnAbrirArchivo.addActionListener(this);
        try {
            connect();
        } catch (Exception ex) {
            Logger.getLogger(controlador.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private synchronized void enviar() {
        if (view.ckArchivo.isSelected()) {
            convertirBinarioArchivo();
        } else {
            convertirBinario();
        }

        agregarRedundancia();
        entramar();
        transporte();
    }

    private synchronized void convertirBinarioArchivo() {
        ArrayList by = new ArrayList();
        try {
            byte[] fileContent = Files.readAllBytes(Paths.get(this.path));
            for (byte o : fileContent) {

                by.add("" + o);
                //System.out.println(o);
            }
        } catch (IOException ex) {

        }

        for (Object text : by) {
            String cosa = Integer.toBinaryString(Integer.parseInt((String) text));
            int ajuste = 32 - cosa.length();
            String ajus = "";
            for (int contAjuste = 0; contAjuste < ajuste; contAjuste++) {
                ajus = ajus + "0";
            }
            ajus = ajus + cosa;
            textoBinario.add(ajus);
            //System.out.println(ajus);
        }

    }

    public synchronized void connect() throws Exception {
        CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier("COM3");
        if (portIdentifier.isCurrentlyOwned()) {
            System.out.println("Error: Port is currently in use");
        } else {
            CommPort commPort = portIdentifier.open(this.getClass().getName(), 2000);

            if (commPort instanceof SerialPort) {
                SerialPort serialPort = (SerialPort) commPort;
                serialPort.setSerialPortParams(1500, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

                entrada = serialPort.getInputStream();
                salida = serialPort.getOutputStream();

            } else {
                System.out.println("Error: Only serial ports are handled by this example.");
            }
        }
    }

    private synchronized void convertirBinario() {
        textoBinario = convertir.binary(this.view.txtAreaMensajeSalida.getText());
        try {
            sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(controlador.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private synchronized void agregarRedundancia() {
        textoRedundancia = convertir.redundancia(textoBinario);
        for (Object men : textoRedundancia) {
            this.view.txtAreaRedundancia.append((String) men + "\n");
        }
        try {
            sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(controlador.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private synchronized void entramar() {
        textoEntramado = convertir.entramado(textoRedundancia);
        for (Object men : textoEntramado) {
            this.view.txtAreaEntramado.append((String) men + "\n");
        }
        try {
            sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(controlador.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private synchronized void transporte() {
        EnviarHilo enviar = new EnviarHilo();
        Thread hilo = new Thread(enviar);
        hilo.start();
        try {
            try {
                enviar.transporte(view, textoEntramado, error, perdida, DELAY, this.entrada, this.salida);
            } catch (IOException ex) {
                Logger.getLogger(controlador.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(controlador.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private synchronized void servidor() {
        RecibirHilo recibir = new RecibirHilo();
        Thread hilo = new Thread(recibir);
        hilo.start();
        recibir.servidor(this.view, this.entrada, this.salida);
    }

    @Override
    public synchronized void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(view.btnEnviar)) {

            if (this.view.ckError.isSelected()) {
                error = 1;

            } else {
                error = 0;
            }

            if (this.view.ckPerdida.isSelected()) {
                perdida = 1;
            } else {
                perdida = 0;
            }

            this.DELAY = this.view.sliderVelocidad.getValue();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    enviar();
                }
            }).start();

        } else if (e.getSource().equals(view.btnArchivo)) {

            JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());

            int returnValue = jfc.showOpenDialog(null);
            // int returnValue = jfc.showSaveDialog(null);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedFile = jfc.getSelectedFile();
                path = selectedFile.getAbsolutePath();
                view.txtArchivo.setText(path);
            }
        }

        if (e.getSource().equals(view.ckCRC)) {

            this.view.ckHamm.setSelected(false);
        } else if (e.getSource().equals(view.ckHamm)) {
            this.view.ckCRC.setSelected(false);
        }

        if (e.getSource().equals(view.btnLimpiar)) {
            servidor();
            view.jListArchivosRecibidos.clearSelection();
        }

        if (e.getSource().equals(view.ckArchivo)) {
            if (view.ckArchivo.isSelected()) {
                view.txtArchivo.setEnabled(true);
                view.txtSalida.setEnabled(true);
                view.btnArchivo.setEnabled(true);
            } else {
                view.txtArchivo.setEnabled(false);
                view.txtSalida.setEnabled(false);
                view.btnArchivo.setEnabled(false);
            }

        }

        if (e.getSource().equals((view.btnRecibir))) {
            (new Thread(new Runnable() {
                //private ServerSocket serverSocket;
                @Override
                public void run() {
                    servidor();
                }
            })).start();
        }
        
        if(e.getSource().equals(view.btnAbrirArchivo)) {
            fileManager.abrirArchivo();
        }
    }    
    

}
