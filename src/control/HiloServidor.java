package control;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class HiloServidor extends Thread
{

	private Socket sktControl;
	private PrintWriter out;

	public HiloServidor(Socket clientSocket)
	{
		try
		{
			this.sktControl = clientSocket;
			out = new PrintWriter(clientSocket.getOutputStream(), true);
		} catch (IOException e)
		{
			System.out.println("Error creando el flujo de transmisión");
		}

	}

	public void run()
	{
		System.out.println("Got a client");
		while (true)
		{
			if (sktControl.isClosed())
			{
			} else
			{
				try
				{

					BufferedReader in = new BufferedReader(new InputStreamReader(sktControl.getInputStream()));
					// Mientras el buffer está vacío
					while (!in.ready())
					{
					}

					String comando = in.readLine();

					if (comando.equalsIgnoreCase("PASV"))
					{
						out.println("OK");
					}
					// Finalizar la conexión
					else if (comando.equalsIgnoreCase("END"))
					{
						out.close();
					}
					// En caso de que no se conozca el comando
					else
					{
						out.println("ERR");
					}

				} catch (IOException e)
				{
					System.out.println("Error en el flujo de información");
				}
			}

		}

	}

}
