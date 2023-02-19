package com.example.clientprova;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.text.TextFlow;
import model.Client;
import model.Email;


import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WriteEmailController {

    @FXML
    public Label lblData;
    @FXML
    public Label lblsenderAccount;
    @FXML
    public TextField lblTo;
    @FXML
    public TextField lblSubject;
    @FXML
    public TextArea txtEmail;

    boolean checkBodyEmail = false;
    Email mail;
    Client model;
    MainController mainController;
    ClientController clientController;
    StringProperty receivers;
    /*
    * action può assumere valore "forward", "reply", "replyall"
    * textOrSubject prende il testo della mail se action=="forward", altrimenti oggetto della mail
    * */
    public void setMainController(MainController m, Client model, ClientController clientController, String action, Email mail){
        this.mainController = m;
        this.model = model;
        this.mail = mail;
        this.clientController = clientController;
        lblsenderAccount.textProperty().bind(model.emailAddressProperty());
        Date date = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        String strDate = dateFormat.format(date);
        StringProperty data = new SimpleStringProperty(strDate);
        lblData.textProperty().bind(data);
        lblTo.setPromptText("Invia a: ");
        lblSubject.setPromptText("Oggetto");
        setwriteEmail(action);
    }




    @FXML
    public void sendEmail(ActionEvent actionEvent) {
        String pattern = "ddMMyyyyHHmmss";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        String date=simpleDateFormat.format(new Date());

        String uniqueID = date + "_" + UUID.randomUUID();
        if((txtEmail.getText() != "" && lblSubject.getText()!="") || checkBodyEmail) {
            ArrayList<String> receivers = getReceivers();
            if (receivers.size() > 0) {
                lblTo.setStyle("-fx-border-color: none;");
                String checkReceivers = checkReceivers(receivers);
                if (checkReceivers == "") {
                    Email send = new Email(uniqueID, lblData.getText(), lblsenderAccount.getText(), receivers, lblSubject.getText(), txtEmail.getText(), true);
                    clientController.sendEmail(send,response->{
                        if(response.getStatus()=="OK"){
                            Platform.runLater(()-> {
                                txtEmail.setText("");
                                lblSubject.setText("");
                                lblTo.setText("");
                            });
                        }else  Platform.runLater(()-> mainController.loadAlert("Qualcosa è andato storto",response.getMsg(),"ERROR", ""));

                    });
                    model.setWriting(false);
                }
//                    System.out.println(feedback);

                 else {
                    lblTo.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                    Alert a = new Alert(Alert.AlertType.ERROR);
                    a.setContentText("L'utente " + checkReceivers + " non esiste. Impossibile inviare mail");
                    a.show();
                }
            } else {
                lblTo.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setContentText("Aggiungi almeno un destinatario");
                a.show();
            }
        } else {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setContentText("Stai scordando di scrivere qualcosa");
            a.setTitle("Errori nella scrittura dell'email");
            a.setHeaderText("Errori email");
            a.show();
            checkBodyEmail = true;
        }
    }
    private ArrayList<String> getReceivers(){
        ArrayList<String> receivers = new ArrayList<>();
        if(lblTo.getText().length()>0) {
            String[] receiv = lblTo.getText().split("\\s+");
            for (int i = 0; i < receiv.length; i++) {
                receivers.add(receiv[i]);
                System.out.println(receivers.get(i));
            }
        }
        return receivers;
    }

    private String checkReceivers(ArrayList<String> r){
        boolean errorReceivers=true;
        for(int i = 0; i<r.size(); i++){
            errorReceivers = errorReceivers && new File("clientprova/src/main/resources/com/example/clientprova/"+r.get(i)).exists();

            if(!errorReceivers){
                return r.get(i);
            }
        }
        return "";
    }

    public void clearWriteEmail(){
        lblTo.setText("");
        lblTo.setEditable(true);
        lblSubject.setText("");
        lblSubject.setEditable(true);
        txtEmail.setText("");
        txtEmail.setEditable(true);
    }

    public void setwriteEmail(String action){
        switch(action){
            case "forward":
                clearWriteEmail();
                String txtForwarded="-----Forwarded message-----\n" +
                        "From: "+mail.getSender()+"\n"+
                        "To: "+mail.getReceivers()+ "\n"+
                        "Date: "+mail.getDataSpedizione()+"\n"+
                        "Subject: "+mail.getSubject()+"\n";

                txtEmail.setText("\n\n"+txtForwarded + mail.getText());
                txtEmail.positionCaret( 0 );
                lblSubject.setText(mail.getSubject());
                lblSubject.setEditable(false);
                lblTo.requestFocus();
                break;

            case "reply":
                clearWriteEmail();
                lblTo.setText(mail.getSender());
                lblTo.setEditable(false);
                lblSubject.setText("Re: " + mail.getSubject());
                lblSubject.setEditable(false);
                break;

            case "replyAll":
                clearWriteEmail();
                ArrayList<String> rec = mail.getReceivers();
                String receiver = mail.getSender();
                for(int i=0; i<rec.size(); i++){
                    if(!rec.get(i).equals(model.getEmailAddress()))
                        receiver += " " + (rec.get(i));
                }
                System.out.println(receiver);
                lblTo.setText(receiver);
                lblTo.setEditable(false);
                lblSubject.setText("Re: " + mail.getSubject());
                lblSubject.setEditable(false);
                break;

            case "clear":
                clearWriteEmail();
                model.setWriting(false);
                break;
        }

    }
}

