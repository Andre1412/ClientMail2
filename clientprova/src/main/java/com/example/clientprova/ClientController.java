package com.example.clientprova;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.util.converter.LocalDateTimeStringConverter;
import model.Client;
import model.Email;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;


import static java.lang.Thread.sleep;

public class ClientController {

    private final ExecutorService threadPool;
    private MainController mainController;
    Client client;
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private BooleanProperty serverStatus;

    boolean firstConn=true;

    Socket socket;
    ObjectOutputStream outputStream = null;
    ObjectInputStream inputStream = null;

    public ClientController(Client client, MainController mainController) {
        threadPool = Executors.newFixedThreadPool(10);
        this.client = client;
        this.serverStatus = new SimpleBooleanProperty(false);
        this.mainController = mainController;
    }

    public void communicate(String host, int port){

        //while(!success && attempts <= 5) {


            scheduler.scheduleAtFixedRate(() ->{
                if(firstConn) {
                    firstConn = !tryCommunication(host, port, firstConn);
                }else tryCommunication(host, port, firstConn);
            },0, 5, TimeUnit.SECONDS);
            /*if(success) {
                //tryConnect(host, port);
                continue;
            } */
        //}
        /*if(!success)
            throw new RuntimeException("Server irraggiungibile");*/
    }



    private boolean tryCommunication(String host, int port, boolean firstTime){
        try {
            connectToServer(host, port);
            outputStream.writeUTF("receive");
            outputStream.writeUTF(client.getEmailAddress());
            outputStream.writeUTF(firstTime?"null":client.getLastEmailFormattedDate());
            outputStream.flush();
            //Se è la prima connessione aspetto anche mail inviate
            if(firstTime) {
                ArrayList<Email> sentEmails = (ArrayList<Email>) inputStream.readObject();
                Platform.runLater(()->
                        client.setSentContent(sentEmails));
            }
            //Attendo mail in arrivo
            ArrayList<Email> inboxEmails = (ArrayList<Email>)inputStream.readObject();

            Platform.runLater(()->
                client.setInboxContent(inboxEmails));

            return true;
        } catch (IOException e) {
            System.out.println("Connessione fallita");
            this.serverStatus.setValue(false);
            return false;
        } catch (ClassNotFoundException e) {
            System.out.println("Errori nella lettura dei dati");
            return false;
        }
        finally {
            closeConnections();
        }
    }

    private void closeConnections() {
        if (socket != null) {
            try {
                inputStream.close();
                outputStream.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void connectToServer(String host, int port) throws IOException {
            socket = new Socket(host, port);
            //TODO: serve ancora?
            //socket.setSoTimeout(2*1000);
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.flush();
            inputStream = new ObjectInputStream(socket.getInputStream());
            this.serverStatus.setValue(true);
    }

    public interface ResponseFunction {
        void run(ServerResponse response);
    }
    public void sendEmail(Email send, ResponseFunction response) {
        threadPool.execute(()->{
        try {

            this.connectToServer("localhost", 8085);
            outputStream.writeUTF("send");
            outputStream.writeUTF(client.getEmailAddress());
            outputStream.writeObject(send);
            outputStream.flush();
            String feedback=inputStream.readUTF();
            ServerResponse res=new ServerResponse(feedback.contains("ERROR")?"ERROR": "OK", feedback);
            if(res.getStatus()=="OK"){
                ArrayList<Email> sendArray=new ArrayList<>();
                sendArray.add(send);
                client.setSentContent(sendArray);
            }
            response.run(res);

//            return feedback;
        } catch (IOException e) {
            System.out.println("Errore comunicazione: "+ e.getMessage());
            this.serverStatus.setValue(false);
            response.run(new ServerResponse("ERROR","Errore di comunicazione"));
//            return "Errore nella comunicazione";
        }finally{
            closeConnections();
        }

        });
    }

    public void setToRead(Email email){
        threadPool.execute(()-> {
            try {
                this.connectToServer("localhost", 8085);
                outputStream.writeUTF("read");
                outputStream.writeUTF(client.getEmailAddress());
                outputStream.writeObject(email);
                outputStream.flush();
                if (inputStream.readUTF().contains("ERROR")) {
                    System.out.println("Errore nella modifica");
                } else System.out.println("Modificato con successo!");
            } catch (IOException e) {
                System.out.println("Errore comunicazione: " + e.getMessage());
                this.serverStatus.setValue(false);
            } finally {
                System.out.println("Chiudo");
                closeConnections();
            }
        });
    }
    public void deleteEmail(String email, ResponseFunction response){
        threadPool.execute(()->{
            try{
                this.connectToServer("localhost", 8085);
                //this.serverStatus.setValue(true);
                outputStream.writeUTF("delete");
                outputStream.writeUTF(client.getEmailAddress());
                outputStream.writeUTF(email);
                outputStream.flush();
                String feedback=inputStream.readUTF();

                ServerResponse res=new ServerResponse(feedback.contains("ERROR")? "ERROR":"OK", feedback);
                response.run(res);
            }catch (IOException e){
                System.out.println("Errore comunicazione: "+ e.getMessage());
                this.serverStatus.setValue(false);
                response.run(new ServerResponse("ERROR", "Errore di comunicazione"));
            }finally{
                System.out.println("Chiudo");
                closeConnections();
            }
        });
    }

    public BooleanProperty serverStatusProperty() {
        return serverStatus;
    }


    public void closeThreadpool(){
        scheduler.shutdownNow();
        threadPool.shutdownNow();
    }

}
