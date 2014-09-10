package control;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.Socket;


public class HiloServidor extends Thread 
{

	// Servicio de paquetes no orientado a conexión
	DatagramPacket dPacketEnvia;
	// Puerto fuente
	int puerto = 0;
	// Dirección fuente
	InetAddress direccion = null;
	
	Socket clientSocket;


	public HiloServidor(Socket clientSocket) 
	{
		this.clientSocket = clientSocket;
	}

	public void run() 
	{
		System.out.println("Got a client");
		while (true) 
		{
			if(clientSocket.isClosed())
			{
			}
			else
			{
				try
				{
					BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
					//Mientras el buffer está vacío
					while(!in.ready())
					{
					}
					System.out.println(in.readLine());
					System.out.println("\n");

				} catch (IOException e)
				{
					e.printStackTrace();
				}
			}


		}

	}
	

	
}
