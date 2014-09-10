package control;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class ServerFTP
{

	public static void main(String[] args)
	{

		// Socket para conexión de cliente - TCP
		ServerSocket controlSocketServidor = null;
		ServerSocket dataSocketServidor = null;
		try
		{
			controlSocketServidor = new ServerSocket(4000);
			dataSocketServidor = new ServerSocket(4001);
			System.out.println("Esperando por clientes...");
			while (true)
			{

				// Crear y asignar la conexión de datos a cada cliente
				ServerDTP hiloDTP = new ServerDTP(dataSocketServidor);

				// Crear y asignar la conexión de control a cada cliente
				Socket controlSocket = controlSocketServidor.accept();
				ServerPI hiloPI = new ServerPI(controlSocket, hiloDTP);
				hiloPI.start();
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
