package control;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class Servidor 
{
	
	public static void main(String[]args)
	{
		//Socket
		DatagramSocket dSocketServidor=null;
		try 
		{
			dSocketServidor=new DatagramSocket(4000);
			System.out.println("Esperando por clientes...");
			while(true)
			{
				//Recibir clientes y asignárselos a un hilo
				byte buff[]=new byte[250];
				DatagramPacket dPacketRecibe=new DatagramPacket(buff, buff.length);
				dSocketServidor.receive(dPacketRecibe);
				HiloServidor hilo=new HiloServidor(dPacketRecibe);
				hilo.start();
				System.out.println("Conectado a "+dPacketRecibe.getAddress());
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
