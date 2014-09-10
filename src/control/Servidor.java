package control;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class Servidor 
{
	
	public static void main(String[]args)
	{
		//Socket para conexión de cliente - TCP		
		ServerSocket dSocketServidor=null;
		try 
		{
			dSocketServidor=new ServerSocket(4000);
			System.out.println("Esperando por clientes...");
			while(true)
			{
				Socket clientSocket = dSocketServidor.accept();
				
				
				//Recibir clientes y asignárselos a un hilo
				HiloServidor hilo=new HiloServidor(clientSocket);
				hilo.start();
			}
			
			
		} catch (SocketException e) 
		{
			System.out.println("Error creando el socket");
		} catch (IOException e) 
		{
			System.out.println("Error en el flujo de información");
		}
	}
	
	

}
