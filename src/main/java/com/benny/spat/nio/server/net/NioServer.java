package com.benny.spat.nio.server.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import com.benny.spat.nio.server.IServer;

public class NioServer implements IServer{
	
	private Selector selector;

	public void start(int port) {
		try {
			this.selector = Selector.open();
			ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.bind(new InetSocketAddress(port));
			serverSocketChannel.configureBlocking(false);
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
			
			new Thread(new NIOServer()).start();
			System.out.println("NIO服务器已启动...");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public class NIOServer implements Runnable{

		public void run() {
			while(true) {
				try {
					int select = selector.select();
					if(select <= 0) {
						continue;
					}
					
					Set<SelectionKey> selectedKeys = selector.selectedKeys();
					Iterator<SelectionKey> iterator = selectedKeys.iterator();
					while(iterator.hasNext()) {
						SelectionKey selectionKey = iterator.next();
						iterator.remove();
						if (!selectionKey.isValid()) {
							System.out.println("该信道已失效！");
							
							SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
							socketChannel.close();
							selectionKey.interestOps(SelectionKey.OP_ACCEPT);
						}else if(selectionKey.isAcceptable()) {
							ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
							SocketChannel accept = serverSocketChannel.accept();
							accept.configureBlocking(false);  
							accept.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);  
						}else if(selectionKey.isReadable()) {
							ByteBuffer byteBuffer = ByteBuffer.allocate(512);
							SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
							try {
								while(socketChannel.read(byteBuffer) > 0) {
									byteBuffer.flip();
									byte[] array = byteBuffer.array();
									byteBuffer.clear();
									System.out.println(new String(array));
								}
								selectionKey.interestOps(SelectionKey.OP_READ);
							}catch(Exception e) {
								selectionKey.cancel();
								SocketChannel channel = (SocketChannel) selectionKey.channel();
//								channel.socket().close();
								channel.close();
							}
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
		}
		
	}
	
	public static void main(String[] args) {
		NioServer server = new NioServer();
		server.start(8080);
	}
	
}
