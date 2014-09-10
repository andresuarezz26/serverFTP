package control;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;


public class HiloServidor extends Thread 
{
	// Socket
	DatagramSocket dSocketHilo;
	// Servicio de paquetes no orientado a conexión
	DatagramPacket dPacketRecibe, dPacketEnvia;
	// Puerto fuente
	int puerto = 0;
	// Dirección fuente
	InetAddress direccion = null;


	public HiloServidor(DatagramPacket dPacketRecibe) 
	{
		try 
		{
			dSocketHilo = new DatagramSocket();
		} catch (SocketException e) 
		{
			System.out.println("Error creando el socket");
		}

		// Asignar valores a las variables
		this.dPacketRecibe = dPacketRecibe;
		puerto = dPacketRecibe.getPort();
		direccion = dPacketRecibe.getAddress();

		// Respuesta del hilo
		byte[] envio = new byte[250];
		dPacketEnvia = new DatagramPacket(envio, envio.length, direccion, puerto);
		try 
		{
			dSocketHilo.send(dPacketEnvia);
		} catch (IOException e) 
		{
			System.out.println("Error enviando el paquete");
		}
		
	}

	public void run() 
	{
		while (true) 
		{
			try 
			{
				byte[] buzon = new byte[250];
				dPacketRecibe = new DatagramPacket(buzon, buzon.length);
				dSocketHilo.receive(dPacketRecibe);
				String mensaje = new String(dPacketRecibe.getData(), 0, dPacketRecibe.getLength());
				System.out.println("El mensaje es: " + mensaje);
			} catch (IOException e) 
			{
				System.out.println("Error en el flujo");
			} 

		}

	}
	

	
}
