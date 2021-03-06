package com.imgeek.net;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author: xiemin
 * @date: 2018/9/26 20:49
 */
@Slf4j
class SelectProcess<T> implements Runnable {

    private Queue<SocketChannel> socketChannelQueue;

    private Selector readSelector;

//    private Selector writeSelector;

    private ByteBuffer readByteBuffer;

    private ByteBuffer writeByteBuffer;

    private Queue<String> responseQueue;

    private int capacity = 1024;

    private IProtocolHandler iProtocolHandler;

    SelectProcess(Queue socketChannelQueue, IProtocolHandler iProtocolHandler) throws IOException {
        this.socketChannelQueue = socketChannelQueue;
        this.readSelector = Selector.open();
//        this.writeSelector = Selector.open();
        this.readByteBuffer = ByteBuffer.allocate(capacity);
        this.writeByteBuffer = ByteBuffer.allocate(capacity);
        this.responseQueue = new LinkedBlockingQueue<>();
        this.iProtocolHandler = iProtocolHandler;
    }

    @Override
    public void run() {
        while (true) {
            try {
                registerReadSocket();
                readFromSockets();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private void registerReadSocket() throws IOException {
        SocketChannel socketChannel = socketChannelQueue.poll();
        while (socketChannel != null) {
            log.info("register");
            socketChannel.configureBlocking(false);
            SelectionKey key = socketChannel.register(readSelector, SelectionKey.OP_READ);
            key.attach(socketChannel);
            socketChannel = socketChannelQueue.poll();
        }
    }

    private void readFromSockets() throws IOException {
        int readyRead = readSelector.selectNow();
        if (readyRead > 0) {
            Set<SelectionKey> selectionKeySet = readSelector.selectedKeys();
            Iterator<SelectionKey> itr = selectionKeySet.iterator();
            log.info("readyRead : {}, itrSize: {}", readyRead, selectionKeySet.size());
            while (itr.hasNext()) {
                SelectionKey selectionKey = itr.next();
                readFromSocket(selectionKey);
                writeToSocket(selectionKey);
                itr.remove();
            }
            selectionKeySet.clear();
        }
    }

    private void readFromSocket(SelectionKey selectionKey) {
        log.info("read_from_socket");

        try {
            SocketChannel socketChannel = (SocketChannel) selectionKey.attachment();
            read(socketChannel, readByteBuffer);
        } catch (Exception e) {
            log.info(e.getMessage());
            unRegisterReadSocket(selectionKey);
        } finally {
            log.info("readByteBuffer clear");
            readByteBuffer.clear();
        }
    }

    private int read(SocketChannel socketChannel, ByteBuffer byteBuffer) throws IOException {
        int bytesReaded = 0;
        int totalBytesReaded = bytesReaded;

        bytesReaded = socketChannel.read(byteBuffer);
        totalBytesReaded += bytesReaded;
        while (bytesReaded > 0) {
            bytesReaded = socketChannel.read(byteBuffer);
            totalBytesReaded += bytesReaded;
        }

        //receive client socket close
        if (bytesReaded == -1) {
            throw new IOException("socketchannel read ret bytesReaded:".concat(String.valueOf(bytesReaded)));
        }

        //print client sent message
        byteBuffer.flip();
        String requestData = byteBufferToString(readByteBuffer);
        String responseData = iProtocolHandler.handle(requestData);
        responseQueue.offer(responseData);
        log.info(String.format("totalBytesReaded : %s, receive %s from client.", totalBytesReaded, requestData));
        return totalBytesReaded;
    }

    private void unRegisterReadSocket(SelectionKey selectionKey) {
        log.info("unRegisterReadSocket");
        selectionKey.attach(null);
        selectionKey.cancel();
        try {
            selectionKey.channel().close();
        } catch (IOException e) {
            log.info(e.getMessage(), e);
        }
    }

//    private void registerWriteSocket(SelectionKey selectionKey) throws ClosedChannelException {
//        SocketChannel socketChannel = (SocketChannel) selectionKey.attachment();
//        if (socketChannel != null) {
//            log.info("register write socket");
//            socketChannel.register(writeSelector, SelectionKey.OP_WRITE, socketChannel);
//        }
//    }

//    private void writeToSockets() throws IOException {
//        int readyWrite = writeSelector.selectNow();
//        log.debug("write_from_socket, readyWrite:{}", readyWrite);
//        if (readyWrite > 0) {
//            Set<SelectionKey> selectionKeys = writeSelector.selectedKeys();
//            Iterator<SelectionKey> itr = selectionKeys.iterator();
//            while (itr.hasNext()) {
//                SelectionKey key = itr.next();
//                writeToSocket(key);
//                itr.remove();
//            }
//            selectionKeys.clear();
//        }
//    }

    private void writeToSocket(SelectionKey key) throws IOException {
        log.debug("writeToSocket");
        int totalBytesWriten = 0;
        String httpResponseStr = responseQueue.poll();
        if (httpResponseStr != null) {
            SocketChannel socketChannel = (SocketChannel) key.attachment();
            if (socketChannel != null) {
                writeByteBuffer.put(httpResponseStr.getBytes());
                writeByteBuffer.flip();
                totalBytesWriten = write(socketChannel, writeByteBuffer);
                writeByteBuffer.clear();
            }
            log.info("totalBytesWriten :{}", totalBytesWriten);
        }
    }

    private int write(SocketChannel socketChannel, ByteBuffer byteBuffer) throws IOException {
        int bytesWriten = 0;
        int totalBytesWriten = bytesWriten;
        bytesWriten = socketChannel.write(byteBuffer);
        totalBytesWriten += bytesWriten;
        while (bytesWriten > 0) {
            bytesWriten = socketChannel.write(byteBuffer);
            totalBytesWriten += bytesWriten;
        }
        return totalBytesWriten;
    }

//    private String byteBufferToLine(ByteBuffer readByteBuffer) {
//        int j;
//        char CR = '\r';
//        char LF = '\n';
//        byte[] bytes = readByteBuffer.array();
//        for (j = 1; j < bytes.length; j++) {
//            if (bytes[j - 1] == CR && bytes[j] == LF) break;
//        }
//        return (new String(bytes)).substring(0, j);
//    }

    private String byteBufferToString(ByteBuffer readByteBuffer) {
        return new String(readByteBuffer.array()).substring(0, readByteBuffer.limit());
    }
}