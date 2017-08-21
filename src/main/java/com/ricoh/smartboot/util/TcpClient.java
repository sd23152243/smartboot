package com.ricoh.smartboot.util;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Scanner;

public class TcpClient
{
	//定义检测SocketChannel的Selector对象
	private Selector selector = null;
	//定义处理编码和解码的字符集
	private Charset charset = Charset.forName("UTF-8");
	//客户端SocketChannel
	private SocketChannel sc = null;
	public void init()throws IOException
	{
		selector = Selector.open();
		InetSocketAddress isa = new InetSocketAddress("172.31.76.154", 30002);
		//调用open静态方法创建连接到指定主机的SocketChannel
		sc = SocketChannel.open(isa);
		//设置该sc以非阻塞方式工作
		sc.configureBlocking(false);
		//将SocketChannel对象注册到指定Selector
		sc.register(selector, SelectionKey.OP_READ);
		//启动读取服务器端数据的线程
		new ClientThread().start();
		//创建键盘输入流
		Scanner scan = new Scanner(System.in);
		while(scan.hasNextLine())
		{
			//读取键盘输入
			String line = scan.nextLine();
			//将键盘输入的内容输出到SocketChannel中
			sc.write(charset.encode(line));
		}
	}
	//定义读取服务器数据的线程
	private class ClientThread extends Thread
	{
		public void run()
		{
			try
			{
				while(selector.select() > 0)
				{
					//遍历每个有可用IO操作Channel对应的SelectionKey
					for(SelectionKey sk : selector.selectedKeys())
					{
						//删除正在处理的SelectionKey
						selector.selectedKeys().remove(sk);
						//如果该SelectionKey对应的Channel中有可读的数据
						if(sk.isReadable())
						{
							//使用NIO读取Channel中的数据
							SocketChannel sc = (SocketChannel)sk.channel();
							ByteBuffer buff = ByteBuffer.allocate(1024);
							String content = "";
							while(sc.read(buff) > 0)
							{
								sc.read(buff);
								buff.flip();
								content += charset.decode(buff);
							}
							System.out.println("message: " + content);
							//为下一次读取作准备
							sk.interestOps(SelectionKey.OP_READ);
						}
					}
				}
			}
			catch(IOException ex)
			{
				ex.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args)throws IOException
	{
		new TcpClient().init();
	}
}