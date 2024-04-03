package Cliente;
import Criptografia.CriptoRSA;
import Criptografia.CriptografiaAES;
import Criptografia.ImplSHA3;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.net.Socket;
import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Cliente implements Runnable{

    private Socket cliente;
    private boolean conexao1 = true;
    private boolean conexaoTrocaDeChavesPublicaRSA = true;
    private boolean conexaoParaDistribuicaoChaveAES = true;
    private boolean conexaoParaTrocaDeMensagens = true;
    private PrintStream saida;
    private ObjectInputStream in;
    private DataInputStream inChaveAES;

    private String algoritmoHash;
    private String resultadoDoHash;
    private String hashCifradaComRSA;

    private CriptografiaAES criptoAES;

    private CriptoRSA criptoRSA = new CriptoRSA();
    private String ChavePublicaServidor;                    //armazena a chave publica do servidor
    private SecretKey chaveAESServidor;


    BigInteger eServidor = new BigInteger("123123");
    BigInteger nServidor = new BigInteger("123213");
    

    public Cliente(Socket c){
        this.cliente = c;
    }

    @Override
    public void run() {
        try {
            System.out.println("O cliente conectou ao servidor");

            Scanner teclado = new Scanner(System.in);

            //se o cliente for receber algo do servidor
            //ver video Mini Tutorial Servidor de Eco Multithread em Java
            //parte 7:48 ele fala como fazer

            //algoritmo hash usado
            algoritmoHash = "SHA-256";

            //Armazena os bytes do hash do texto cifrado do algoritmo AES
            byte[] hashDoTextoCifradoAES;

            BigInteger dServidor = new BigInteger("123");

            //canal de envio de mensagens
            saida = new PrintStream(cliente.getOutputStream());

            //canal de recebimento de mensagens
            Scanner s = null;
            s = new Scanner(cliente.getInputStream());         
            
            in = new ObjectInputStream(cliente.getInputStream());

            inChaveAES = new DataInputStream(cliente.getInputStream());

            //armazena temporariamente a mensagem que sera enviada ao servidor, antes de ser cifrada
            String MensagemTemporaria;

            //armazena mensagem que sera enviada ao servidor
            String MensagemEnviada;

            //armazena numeros inteiros em modo texto
            String inteiroParaTexto;


            //armazena a mensagem enviada pelo servidor, contendo o hash do AES cifrado em RSA
            String hashDoAESCifradoRSA;

            //armazena a mensagem enviada pelo servidorr, cifrada em AES
            String mensagemCifradaAES;

            //armazena hash do AES decifrado
            String hashDoAESDecifrado;
            
            //armazena mensagem AES decifrada
            String decifraAESDaMensagem = "";

            //armazena o resultado da confirmação de login do servidor
            int confirmarLogin = -1;


            //armazena as partes da chave publica do cliente
            String letraE,LetraN, LetraEeLetraN;
             
            
            //Troca de chaves publica do RSA entre cliente e servidor
            if (conexaoTrocaDeChavesPublicaRSA) {

                //Recebe chave publica do servidor
                //que será usada para cifrar a mensagem do cliente para o servidor
                //o servidor usará sua chave privada para decfifrar a mensagem
                ChavePublicaServidor = s.nextLine();

                //Pega chave do servidor
                //tira a concatenação, alterar 
                String[] array = ChavePublicaServidor.split(" ");

                String LetraEServidor = array[0];
                String LetraNServidor = array[1];

                eServidor = new BigInteger(LetraEServidor);
                nServidor = new BigInteger(LetraNServidor);

                

                //Gera chave publica do cliente
                //recebe os valores
                BigInteger e = criptoRSA.enviarE();
                BigInteger n = criptoRSA.enviarN();
                BigInteger d = criptoRSA.enviarD();



                dServidor = criptoRSA.enviarD();

                //converte para string
                letraE = e.toString();
                LetraN = n.toString();
                
                //concateno ambos
                LetraEeLetraN = letraE+ " " +LetraN;
                
                //Envia chave publica do cliente para o servidor
                saida.println(LetraEeLetraN);

                conexaoTrocaDeChavesPublicaRSA = false;
            }

            //recebimento de chave do AES enviada pelo servidor
            if (conexaoParaDistribuicaoChaveAES) {
                
                String m = s.nextLine();
                
                String chaveDecifrada = criptoRSA.desencriptar(m, criptoRSA.enviarD(), criptoRSA.enviarN());

                byte[] chaveFinal = Base64.getDecoder().decode(chaveDecifrada);

                chaveAESServidor = new SecretKeySpec(chaveFinal, "AES");

                conexaoParaDistribuicaoChaveAES = false;
                    
            };


            //troca de mensagens entre cliente e servidor
            while (conexaoParaTrocaDeMensagens) {
                
                int entrada = 0;

                String cifrado = "";
                while (entrada != 3) {
                    
                
                    System.out.println("|********************************|");
                    System.out.println("|--------------------------------|");
                    System.out.println("|###Escolha o que deseja fazer###|");
                    System.out.println("|--------------------------------|");
                    System.out.println("|Fazer Login - 1                 |");
                    System.out.println("|Criar conta - 2                 |");
                    System.out.println("|Sair        - 3                 |");
                    System.out.print(" Digite: ");
                    entrada = teclado.nextInt();
                    System.out.println("|********************************|");
                    System.out.println();
                    System.out.println();
                    System.out.println();
                    System.out.println("/////////////////////////////////////////////");
                    System.out.println();
                    System.out.println();
                    System.out.println();


                        
                                //----------------------------------------------------------------------------
                                    //Envia AES
                                        //cifrar e enviar

                                        inteiroParaTexto = entrada+"";

                                        try {
                                            cifrado = criptoAES.cifrar(inteiroParaTexto, chaveAESServidor);
                                        } catch (Exception e) {
                                            // TODO Auto-generated catch block
                                            e.printStackTrace();
                                        }
                                        saida.println(cifrado);
                                //----------------------------------------------------------------------------

                                //----------------------------------------------------------------------------
                                    //Envia RSA com hash
                                        //cifrar e enviar
                                            
                                            //Faz o hash do texto cifrado AES
                                            hashDoTextoCifradoAES = ImplSHA3.resumo(cifrado.getBytes(ImplSHA3.UTF_8), algoritmoHash);
                                            resultadoDoHash = ImplSHA3.bytes2Hex(hashDoTextoCifradoAES);

                                            //cifra Hash com RSA
                                            hashCifradaComRSA = criptoRSA.encriptar(resultadoDoHash, eServidor, nServidor);
                                            saida.println(hashCifradaComRSA);
                                //----------------------------------------------------------------------------

                    //Se for fazer login
                    if(entrada == 1){


                        System.out.println("|--------------------------------|");
                        System.out.println("|############ Login #############|");
                        System.out.println("|--------------------------------|");
                        System.out.print("|Numero da conta: ");
                        teclado.nextLine();
                        String numConta = teclado.nextLine();
                        
                        System.out.print("|Senha: ");
                        String senha = teclado.nextLine();
                        
            
                        //Concatena as mensagens
                        String numContaEsenha = numConta+ " " +senha;
            
                        //----------------------------------------------------------------------------
                            //Envia AES
                                //cifrar e enviar
                                try {
                                    cifrado = criptoAES.cifrar(numContaEsenha, chaveAESServidor);
                                } catch (Exception e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                                saida.println(cifrado);
                        //----------------------------------------------------------------------------

                        //----------------------------------------------------------------------------
                            //Envia RSA com hash
                                //cifrar e enviar
                                    
                                    //Faz o hash do texto cifrado AES
                                    hashDoTextoCifradoAES = ImplSHA3.resumo(cifrado.getBytes(ImplSHA3.UTF_8), algoritmoHash);
                                    resultadoDoHash = ImplSHA3.bytes2Hex(hashDoTextoCifradoAES);

                                    //cifra Hash com RSA
                                    hashCifradaComRSA = criptoRSA.encriptar(resultadoDoHash, eServidor, nServidor);
                                    saida.println(hashCifradaComRSA);
                        //----------------------------------------------------------------------------

                        
                        System.out.println("Passou");
                        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
                            //Recebe confirmação do servidor
                            mensagemCifradaAES = s.nextLine();
                            
                            hashDoAESCifradoRSA = s.nextLine();

                            //decifra o RSA do hash
                            hashDoAESDecifrado = criptoRSA.desencriptar(hashDoAESCifradoRSA, criptoRSA.enviarD(), criptoRSA.enviarN());

                            //faz o hash da mensagem cifrada em AES recebida
                            hashDoTextoCifradoAES = ImplSHA3.resumo(mensagemCifradaAES.getBytes(ImplSHA3.UTF_8), algoritmoHash);
                            resultadoDoHash = ImplSHA3.bytes2Hex(hashDoTextoCifradoAES);

                            
                            //verifica se os hash são iguais
                            if (resultadoDoHash.equals(hashDoAESDecifrado)) {
                                //como o hash bateu, entao eu posso fazer a decifragem da mensagem AES e usa-la
                                try {
                                    decifraAESDaMensagem = criptoAES.decifrar(mensagemCifradaAES, chaveAESServidor);

                                    
                                    confirmarLogin = Integer.parseInt(decifraAESDaMensagem);
                                } catch (Exception e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }


                            //se o login for válido
                            if (confirmarLogin == 0 ) {
                                System.out.println();
                                System.out.println();
                                System.out.println();
                                System.out.println("/////////////////////////////////////////////");
                                System.out.println();
                                System.out.println();
                                System.out.println();


                                System.out.println("|********************************|");
                                System.out.println("|#####  Login bem sucedido ######|");


                                int escolha = 0;

                                //esse laço vai verificar oque o usuário deseja fazer no sistema
                                while (escolha != 6) {
                                        System.out.println("|********************************|");
                                        System.out.println("|--------------------------------|");
                                        System.out.println("|Saque ........................ 1|");
                                        System.out.println("|Depósito ..................... 2|");
                                        System.out.println("|Transferência ................ 3|");
                                        System.out.println("|Saldo ........................ 4|");
                                        System.out.println("|Investimento ................. 5|");
                                        System.out.println("|Sair ......................... 6|");
                    
                                        System.out.print("|Digite: ");
                                        escolha = teclado.nextInt();
                    
                                        System.out.println("|--------------------------------|");
                                        System.out.println("|********************************|");
                                        System.out.println();
                                        System.out.println();
                                        System.out.println("////////////////////////////////////////////////");
                                        System.out.println();
                                        System.out.println();

                                        //caso o usuário escolha fazer um saque
                                        if (escolha == 1) {
                                            System.out.println("|********************************|");
                                            System.out.println("|--------------------------------|");
                                            System.out.println("|############# Saque ############|");
                                            System.out.println("|--------------------------------|");
                                            System.out.print("|Valor: ");
                                            double saque = teclado.nextDouble();
                                            //sistema.saque(numConta, saque);
                                            

                                            System.out.println("|--------------------------------|");
                                            System.out.println("|********************************|");
                                            System.out.println();
                                            System.out.println();
                                            System.out.println("////////////////////////////////////////////////");
                                            System.out.println();
                                            System.out.println();
                                        }
                                }
                            }
                            //caso o login não seja válido
                            else{
                                System.out.println();
                                System.out.println();
                                System.out.println();
                                System.out.println("/////////////////////////////////////////////");
                                System.out.println();
                                System.out.println();
                                System.out.println();


                                System.out.println("|********************************|");
                                System.out.println("|--------------------------------|");
                                System.out.println("|#### Credenciais inválidas #####|");
                                System.out.println("|--------------------------------|");
                                System.out.println("|********************************|");
                                System.out.println();
                                System.out.println();
                                System.out.println("////////////////////////////////////////////////");
                                System.out.println();
                                System.out.println();
                            }


                    }

                    //se for criar uma conta
                    else if (entrada == 2) {
                        
                    }
                }
                //descifrar
            
                // System.out.println("Escreva uma mensagem: ");
                // MensagemTemporaria = teclado.nextLine();
                
                // //cifrar
                // String fim = "";
                // try {
                    

                //     String cifrado = criptoAES.cifrar(MensagemTemporaria, chaveAESServidor);
                    
                //     fim = cifrado;
                //     System.out.println("****************");
                //     System.out.println(cifrado);
                //     System.out.println("****************");
                //     //saida.println(cifrado);
                // } catch (Exception e) {
                //     // TODO Auto-generated catch block
                //     e.printStackTrace();
                // }

                // saida.println(fim);
                
            }

            saida.close();
            teclado.close();
            cliente.close();
            System.out.println("Cliente finaliza conexao");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    
}