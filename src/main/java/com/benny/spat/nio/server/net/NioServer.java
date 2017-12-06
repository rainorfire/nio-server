package com.benny.spat.nio.server.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

import com.benny.spat.nio.server.IServer;

public class NioServer implements IServer{
	
	public void start(int port) {
		try {
			Selector selector = Selector.open();
			ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.bind(new InetSocketAddress(port));
			serverSocketChannel.configureBlocking(false);
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
			
			new Thread(new NIOServer(selector)).start();
			System.out.println("NIO服务器已启动...");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public class NIOServer implements Runnable{
		
		private Selector selector;
		
		public NIOServer(Selector selector) {
			this.selector = selector;
		}

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
							SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
							try {
								int byteLength = 0;
								int tmpLength = 0;
								ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
								while((tmpLength = socketChannel.read(lengthBuffer)) > 0) {
									if(tmpLength == 4) {
										lengthBuffer.flip();
										byteLength = lengthBuffer.getInt();
										lengthBuffer.clear();
										break;
									}
								}
								if(byteLength > 0) {
									tmpLength = 0;
									ByteBuffer byteBuffer = ByteBuffer.allocate(byteLength);
									while((tmpLength = socketChannel.read(byteBuffer)) <= byteLength) {
										if(tmpLength == byteLength) {
											byteBuffer.flip();
											byte[] array = byteBuffer.array();
											byteBuffer.clear();
											System.out.println(new String(array));
											break;
										}
									}
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
