/**
 * ISC License (ISC)
 * 
 * Copyright (c) 2014, Sam Lanning <sam@samlanning.com>
 * 
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
 * INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM
 * LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR
 * OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THIS SOFTWARE.
 */

package com.samlanning.tools.cdst.examples;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import com.samlanning.tools.cdst.CDSTException;
import com.samlanning.tools.cdst.CDSTHandler;
import com.samlanning.tools.cdst.CDSTester;
import com.samlanning.tools.cdst.CDSTReadHandler;
import com.samlanning.tools.cdst.CDSTWriteHandler;

/**
 * This example program listens on a specific port for a single connection
 * which it then tests with CDSTester.
 * 
 * Run it and telnet to port 9999
 * @author Sam Lanning <sam@samlanning.com>
 *
 */
public class TelnetServer {

    public static final int PORT = 9999;
    
    public static Socket socket;
    public static BufferedReader out;
    public static PrintWriter in;
    
    public static CDSTester<String, String> tester;
    
    // Launcher
    public static void main(String[] args) throws Exception {
        
        ServerSocket server = new ServerSocket(PORT);
        
        // Wait for a single connection
        System.out.println("Listening");
        socket = server.accept();
        // Close immediately to accept only one connection
        server.close();
        System.out.println("Got Connection");
        
        // Setup Duplex Stream
        out = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        in = new PrintWriter(socket.getOutputStream(), true);
        
        // Initial Message
        in.println("Starting Telnet CDST Test");
        in.println("Try interacting with the console to see how each of the");
        in.println("different tests are validated...");
        
        // Setup the tester with 2 second delays before writes
        tester = new CDSTester<String, String>(2000);
        
        setupTester();
        
        // Start the thread accepting input
        (new OutputListener()).start();
        
        tester.run();
        
        socket.close();
    }
    
    public static void setupTester() throws CDSTException {
        
        tester.setHandler(new Handler());
        tester.setLogLevel(CDSTester.L_ALL);
        
        tester.addInputWrite("Hello");
        tester.addInputWrite("How are you today?");
        
        final Container<String> result = new Container<String>();
        
        tester.addOutputRead(new CDSTReadHandler<String>(){

            @Override
            public void read(String output) throws Exception {
                if(output.equals("Good") || output.equals("Bad")){
                    result.object = output;
                } else {
                    throw new Exception("Not 'Good' or 'Bad'!");
                }
            }
            
        });
        
        tester.addInputWrite(new CDSTWriteHandler<String>() {

            @Override
            public String write() {
                if(result.object.equals("Good"))
                    return "Great to hear!";
                else
                    return "Oh I' sorry to hear that!";
            }
            
        });
        
        tester.addOutputRead("Yourself?");
        
        tester.addInputWrite("Great, thanks for asking!");
        
    }
    
    public static class TCPMessage {
        //TODO
    }
    
    public static class Container<T> {
        public T object;
    }
    
    public static class Handler implements CDSTHandler<String> {

        @Override
        public void fail(String message, Exception trace) {
            System.out.println("Error: " + message);
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void writeToStream(String input) {
            in.println(input);
        }
    }
    
    /**
     * Thread class to listen to the output from the socket (stream)
     * @author Sam Lanning <sam@samlanning.com>
     *
     */
    public static class OutputListener extends Thread {
        public void run(){
            while(true){
                try {
                    tester.readFromStream(out.readLine());
                } catch (Exception e) {
                    System.out.println("Socket Closed");
                    System.exit(1);
                }
            }
        }
    }
}
