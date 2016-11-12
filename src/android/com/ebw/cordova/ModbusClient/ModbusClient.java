/*
 * Decompiled with CFR 0_118.
 */
package ModbusClient;

import Exceptions.ConnectionException;
import Exceptions.FunctionCodeNotSupportedException;
import Exceptions.ModbusException;
import Exceptions.QuantityInvalidException;
import Exceptions.StartingAddressInvalidException;
import ModbusClient.ReceiveDataChangedListener;
import ModbusClient.SendDataChangedListener;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ModbusClient {
    private Socket tcpClientSocket = new Socket();
    protected String ipAddress = "190.201.100.100";
    protected int port = 502;
    private byte[] transactionIdentifier = new byte[2];
    private byte[] protocolIdentifier = new byte[2];
    private byte[] length = new byte[2];
    private byte[] crc = new byte[2];
    private byte unitIdentifier;
    private byte functionCode;
    private byte[] startingAddress = new byte[2];
    private byte[] quantity = new byte[2];
    private boolean udpFlag = false;
    private int connectTimeout = 500;
    private InputStream inStream;
    private DataOutputStream outStream;
    public byte[] receiveData;
    public byte[] sendData;
    private List<ReceiveDataChangedListener> receiveDataChangedListener = new ArrayList<ReceiveDataChangedListener>();
    private List<SendDataChangedListener> sendDataChangedListener = new ArrayList<SendDataChangedListener>();

    public ModbusClient(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public ModbusClient() {
    }

    public void Connect() throws UnknownHostException, IOException {
        if (!this.udpFlag) {
            this.tcpClientSocket.setSoTimeout(this.connectTimeout);
            this.tcpClientSocket = new Socket(this.ipAddress, this.port);
            this.outStream = new DataOutputStream(this.tcpClientSocket.getOutputStream());
            this.inStream = this.tcpClientSocket.getInputStream();
        }
    }

    public void Connect(String ipAddress, int port) throws UnknownHostException, IOException {
        this.ipAddress = ipAddress;
        this.port = port;
        this.tcpClientSocket.setSoTimeout(this.connectTimeout);
        this.tcpClientSocket = new Socket(ipAddress, port);
        this.outStream = new DataOutputStream(this.tcpClientSocket.getOutputStream());
        this.inStream = this.tcpClientSocket.getInputStream();
    }

    public static float ConvertRegistersToFloat(int[] registers) throws IllegalArgumentException {
        if (registers.length != 2) {
            throw new IllegalArgumentException("Input Array length invalid");
        }
        int highRegister = registers[1];
        int lowRegister = registers[0];
        byte[] highRegisterBytes = ModbusClient.toByteArray(highRegister);
        byte[] lowRegisterBytes = ModbusClient.toByteArray(lowRegister);
        byte[] floatBytes = new byte[]{highRegisterBytes[1], highRegisterBytes[0], lowRegisterBytes[1], lowRegisterBytes[0]};
        return ByteBuffer.wrap(floatBytes).getFloat();
    }

    public static float ConvertRegistersToFloat(int[] registers, RegisterOrder registerOrder) throws IllegalArgumentException {
        int[] swappedRegisters = new int[]{registers[0], registers[1]};
        if (registerOrder == RegisterOrder.HighLow) {
            swappedRegisters = new int[]{registers[1], registers[0]};
        }
        return ModbusClient.ConvertRegistersToFloat(swappedRegisters);
    }

    public static int ConvertRegistersToDouble(int[] registers) throws IllegalArgumentException {
        if (registers.length != 2) {
            throw new IllegalArgumentException("Input Array length invalid");
        }
        int highRegister = registers[1];
        int lowRegister = registers[0];
        byte[] highRegisterBytes = ModbusClient.toByteArray(highRegister);
        byte[] lowRegisterBytes = ModbusClient.toByteArray(lowRegister);
        byte[] doubleBytes = new byte[]{highRegisterBytes[1], highRegisterBytes[0], lowRegisterBytes[1], lowRegisterBytes[0]};
        return ByteBuffer.wrap(doubleBytes).getInt();
    }

    public static int ConvertRegistersToDouble(int[] registers, RegisterOrder registerOrder) throws IllegalArgumentException {
        int[] swappedRegisters = new int[]{registers[0], registers[1]};
        if (registerOrder == RegisterOrder.HighLow) {
            swappedRegisters = new int[]{registers[1], registers[0]};
        }
        return ModbusClient.ConvertRegistersToDouble(swappedRegisters);
    }

    public static int[] ConvertFloatToTwoRegisters(float floatValue) {
        byte[] floatBytes = ModbusClient.toByteArray(floatValue);
        byte[] highRegisterBytes = new byte[]{0, 0, floatBytes[0], floatBytes[1]};
        byte[] lowRegisterBytes = new byte[]{0, 0, floatBytes[2], floatBytes[3]};
        int[] returnValue = new int[]{ByteBuffer.wrap(lowRegisterBytes).getInt(), ByteBuffer.wrap(highRegisterBytes).getInt()};
        return returnValue;
    }

    public static int[] ConvertFloatToTwoRegisters(float floatValue, RegisterOrder registerOrder) {
        int[] registerValues;
        int[] returnValue = registerValues = ModbusClient.ConvertFloatToTwoRegisters(floatValue);
        if (registerOrder == RegisterOrder.HighLow) {
            returnValue = new int[]{registerValues[1], registerValues[0]};
        }
        return returnValue;
    }

    public static int[] ConvertDoubleToTwoRegisters(int doubleValue) {
        byte[] doubleBytes = ModbusClient.toByteArrayDouble(doubleValue);
        byte[] highRegisterBytes = new byte[]{0, 0, doubleBytes[0], doubleBytes[1]};
        byte[] lowRegisterBytes = new byte[]{0, 0, doubleBytes[2], doubleBytes[3]};
        int[] returnValue = new int[]{ByteBuffer.wrap(lowRegisterBytes).getInt(), ByteBuffer.wrap(highRegisterBytes).getInt()};
        return returnValue;
    }

    public static int[] ConvertDoubleToTwoRegisters(int doubleValue, RegisterOrder registerOrder) {
        int[] registerValues;
        int[] returnValue = registerValues = ModbusClient.ConvertFloatToTwoRegisters(doubleValue);
        if (registerOrder == RegisterOrder.HighLow) {
            returnValue = new int[]{registerValues[1], registerValues[0]};
        }
        return returnValue;
    }

    public boolean[] ReadDiscreteInputs(int startingAddress, int quantity) throws ModbusException, UnknownHostException, SocketException, IOException {
        if (this.tcpClientSocket == null) {
            throw new ConnectionException("connection Error");
        }
        if (startingAddress > 65535 | quantity > 2000) {
            throw new IllegalArgumentException("Starting adress must be 0 - 65535; quantity must be 0 - 2000");
        }
        boolean[] response = null;
        this.transactionIdentifier = ModbusClient.toByteArray(1);
        this.protocolIdentifier = ModbusClient.toByteArray(0);
        this.length = ModbusClient.toByteArray(6);
        this.unitIdentifier = 0;
        this.functionCode = 2;
        this.startingAddress = ModbusClient.toByteArray(startingAddress);
        this.quantity = ModbusClient.toByteArray(quantity);
        byte[] data = new byte[]{this.transactionIdentifier[1], this.transactionIdentifier[0], this.protocolIdentifier[1], this.protocolIdentifier[0], this.length[1], this.length[0], this.unitIdentifier, this.functionCode, this.startingAddress[1], this.startingAddress[0], this.quantity[1], this.quantity[0], this.crc[0], this.crc[1]};
        if (this.tcpClientSocket.isConnected() | this.udpFlag) {
            if (this.udpFlag) {
                InetAddress ipAddress = InetAddress.getByName(this.ipAddress);
                DatagramPacket sendPacket = new DatagramPacket(data, data.length - 2, ipAddress, this.port);
                DatagramSocket clientSocket = new DatagramSocket();
                clientSocket.setSoTimeout(500);
                clientSocket.send(sendPacket);
                data = new byte[2100];
                DatagramPacket receivePacket = new DatagramPacket(data, data.length);
                clientSocket.receive(receivePacket);
                data = receivePacket.getData();
            } else {
                this.outStream.write(data, 0, data.length - 2);
                if (this.sendDataChangedListener.size() > 0) {
                    this.sendData = new byte[data.length - 2];
                    System.arraycopy(data, 0, this.sendData, 0, data.length - 2);
                    for (SendDataChangedListener hl : this.sendDataChangedListener) {
                        hl.SendDataChanged();
                    }
                }
                data = new byte[2100];
                int numberOfBytes = this.inStream.read(data, 0, data.length);
                if (this.receiveDataChangedListener.size() > 0) {
                    this.receiveData = new byte[numberOfBytes];
                    System.arraycopy(data, 0, this.receiveData, 0, numberOfBytes);
                    for (ReceiveDataChangedListener hl : this.receiveDataChangedListener) {
                        hl.ReceiveDataChanged();
                    }
                }
            }
            if (data[7] == 130 & data[8] == 1) {
                throw new FunctionCodeNotSupportedException("Function code not supported by master");
            }
            if (data[7] == 130 & data[8] == 2) {
                throw new StartingAddressInvalidException("Starting adress invalid or starting adress + quantity invalid");
            }
            if (data[7] == 130 & data[8] == 3) {
                throw new QuantityInvalidException("Quantity invalid");
            }
            if (data[7] == 130 & data[8] == 4) {
                throw new ModbusException("Error reading");
            }
            response = new boolean[quantity];
            for (int i = 0; i < quantity; ++i) {
                int intData = data[9 + i / 8];
                int mask = (int)Math.pow(2.0, i % 8);
                response[i] = (intData = (intData & mask) / mask) > 0;
            }
        }
        return response;
    }

    public boolean[] ReadCoils(int startingAddress, int quantity) throws ModbusException, UnknownHostException, SocketException, IOException {
        if (this.tcpClientSocket == null) {
            throw new ConnectionException("connection Error");
        }
        if (startingAddress > 65535 | quantity > 2000) {
            throw new IllegalArgumentException("Starting adress must be 0 - 65535; quantity must be 0 - 2000");
        }
        boolean[] response = new boolean[quantity];
        this.transactionIdentifier = ModbusClient.toByteArray(1);
        this.protocolIdentifier = ModbusClient.toByteArray(0);
        this.length = ModbusClient.toByteArray(6);
        this.unitIdentifier = 0;
        this.functionCode = 1;
        this.startingAddress = ModbusClient.toByteArray(startingAddress);
        this.quantity = ModbusClient.toByteArray(quantity);
        byte[] data = new byte[]{this.transactionIdentifier[1], this.transactionIdentifier[0], this.protocolIdentifier[1], this.protocolIdentifier[0], this.length[1], this.length[0], this.unitIdentifier, this.functionCode, this.startingAddress[1], this.startingAddress[0], this.quantity[1], this.quantity[0], this.crc[0], this.crc[1]};
        if (this.tcpClientSocket.isConnected() | this.udpFlag) {
            if (this.udpFlag) {
                InetAddress ipAddress = InetAddress.getByName(this.ipAddress);
                DatagramPacket sendPacket = new DatagramPacket(data, data.length, ipAddress, this.port);
                DatagramSocket clientSocket = new DatagramSocket();
                clientSocket.setSoTimeout(500);
                clientSocket.send(sendPacket);
                data = new byte[2100];
                DatagramPacket receivePacket = new DatagramPacket(data, data.length);
                clientSocket.receive(receivePacket);
                data = receivePacket.getData();
            } else {
                this.outStream.write(data, 0, data.length - 2);
                if (this.sendDataChangedListener.size() > 0) {
                    this.sendData = new byte[data.length - 2];
                    System.arraycopy(data, 0, this.sendData, 0, data.length - 2);
                    for (SendDataChangedListener hl : this.sendDataChangedListener) {
                        hl.SendDataChanged();
                    }
                }
                data = new byte[2100];
                int numberOfBytes = this.inStream.read(data, 0, data.length);
                if (this.receiveDataChangedListener.size() > 0) {
                    this.receiveData = new byte[numberOfBytes];
                    System.arraycopy(data, 0, this.receiveData, 0, numberOfBytes);
                    for (ReceiveDataChangedListener hl : this.receiveDataChangedListener) {
                        hl.ReceiveDataChanged();
                    }
                }
            }
            if (data[7] == 129 & data[8] == 1) {
                throw new FunctionCodeNotSupportedException("Function code not supported by master");
            }
            if (data[7] == 129 & data[8] == 2) {
                throw new StartingAddressInvalidException("Starting adress invalid or starting adress + quantity invalid");
            }
            if (data[7] == 129 & data[8] == 3) {
                throw new QuantityInvalidException("Quantity invalid");
            }
            if (data[7] == 129 & data[8] == 4) {
                throw new ModbusException("Error reading");
            }
            for (int i = 0; i < quantity; ++i) {
                int intData = data[9 + i / 8];
                int mask = (int)Math.pow(2.0, i % 8);
                response[i] = (intData = (intData & mask) / mask) > 0;
            }
        }
        return response;
    }

    public int[] ReadHoldingRegisters(int startingAddress, int quantity) throws ModbusException, UnknownHostException, SocketException, IOException {
        if (this.tcpClientSocket == null) {
            throw new ConnectionException("connection Error");
        }
        if (startingAddress > 65535 | quantity > 125) {
            throw new IllegalArgumentException("Starting adress must be 0 - 65535; quantity must be 0 - 125");
        }
        int[] response = new int[quantity];
        this.transactionIdentifier = ModbusClient.toByteArray(1);
        this.protocolIdentifier = ModbusClient.toByteArray(0);
        this.length = ModbusClient.toByteArray(6);
        this.unitIdentifier = 0;
        this.functionCode = 3;
        this.startingAddress = ModbusClient.toByteArray(startingAddress);
        this.quantity = ModbusClient.toByteArray(quantity);
        byte[] data = new byte[]{this.transactionIdentifier[1], this.transactionIdentifier[0], this.protocolIdentifier[1], this.protocolIdentifier[0], this.length[1], this.length[0], this.unitIdentifier, this.functionCode, this.startingAddress[1], this.startingAddress[0], this.quantity[1], this.quantity[0], this.crc[0], this.crc[1]};
        if (this.tcpClientSocket.isConnected() | this.udpFlag) {
            if (this.udpFlag) {
                InetAddress ipAddress = InetAddress.getByName(this.ipAddress);
                DatagramPacket sendPacket = new DatagramPacket(data, data.length, ipAddress, this.port);
                DatagramSocket clientSocket = new DatagramSocket();
                clientSocket.setSoTimeout(500);
                clientSocket.send(sendPacket);
                data = new byte[2100];
                DatagramPacket receivePacket = new DatagramPacket(data, data.length);
                clientSocket.receive(receivePacket);
                data = receivePacket.getData();
            } else {
                this.outStream.write(data, 0, data.length - 2);
                if (this.sendDataChangedListener.size() > 0) {
                    this.sendData = new byte[data.length - 2];
                    System.arraycopy(data, 0, this.sendData, 0, data.length - 2);
                    for (SendDataChangedListener hl : this.sendDataChangedListener) {
                        hl.SendDataChanged();
                    }
                }
                data = new byte[2100];
                int numberOfBytes = this.inStream.read(data, 0, data.length);
                if (this.receiveDataChangedListener.size() > 0) {
                    this.receiveData = new byte[numberOfBytes];
                    System.arraycopy(data, 0, this.receiveData, 0, numberOfBytes);
                    for (ReceiveDataChangedListener hl : this.receiveDataChangedListener) {
                        hl.ReceiveDataChanged();
                    }
                }
            }
            if (data[7] == 131 & data[8] == 1) {
                throw new FunctionCodeNotSupportedException("Function code not supported by master");
            }
            if (data[7] == 131 & data[8] == 2) {
                throw new StartingAddressInvalidException("Starting adress invalid or starting adress + quantity invalid");
            }
            if (data[7] == 131 & data[8] == 3) {
                throw new QuantityInvalidException("Quantity invalid");
            }
            if (data[7] == 131 & data[8] == 4) {
                throw new ModbusException("Error reading");
            }
            for (int i = 0; i < quantity; ++i) {
                byte[] bytes = new byte[]{data[9 + i * 2], data[9 + i * 2 + 1]};
                ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
                response[i] = byteBuffer.getShort();
            }
        }
        return response;
    }

    public int[] ReadInputRegisters(int startingAddress, int quantity) throws ModbusException, UnknownHostException, SocketException, IOException {
        if (this.tcpClientSocket == null) {
            throw new ConnectionException("connection Error");
        }
        if (startingAddress > 65535 | quantity > 125) {
            throw new IllegalArgumentException("Starting adress must be 0 - 65535; quantity must be 0 - 125");
        }
        int[] response = new int[quantity];
        this.transactionIdentifier = ModbusClient.toByteArray(1);
        this.protocolIdentifier = ModbusClient.toByteArray(0);
        this.length = ModbusClient.toByteArray(6);
        this.unitIdentifier = 0;
        this.functionCode = 4;
        this.startingAddress = ModbusClient.toByteArray(startingAddress);
        this.quantity = ModbusClient.toByteArray(quantity);
        byte[] data = new byte[]{this.transactionIdentifier[1], this.transactionIdentifier[0], this.protocolIdentifier[1], this.protocolIdentifier[0], this.length[1], this.length[0], this.unitIdentifier, this.functionCode, this.startingAddress[1], this.startingAddress[0], this.quantity[1], this.quantity[0], this.crc[0], this.crc[1]};
        if (this.tcpClientSocket.isConnected() | this.udpFlag) {
            if (this.udpFlag) {
                InetAddress ipAddress = InetAddress.getByName(this.ipAddress);
                DatagramPacket sendPacket = new DatagramPacket(data, data.length, ipAddress, this.port);
                DatagramSocket clientSocket = new DatagramSocket();
                clientSocket.setSoTimeout(500);
                clientSocket.send(sendPacket);
                data = new byte[2100];
                DatagramPacket receivePacket = new DatagramPacket(data, data.length);
                clientSocket.receive(receivePacket);
                data = receivePacket.getData();
            } else {
                this.outStream.write(data, 0, data.length - 2);
                if (this.sendDataChangedListener.size() > 0) {
                    this.sendData = new byte[data.length - 2];
                    System.arraycopy(data, 0, this.sendData, 0, data.length - 2);
                    for (SendDataChangedListener hl : this.sendDataChangedListener) {
                        hl.SendDataChanged();
                    }
                }
                data = new byte[2100];
                int numberOfBytes = this.inStream.read(data, 0, data.length);
                if (this.receiveDataChangedListener.size() > 0) {
                    this.receiveData = new byte[numberOfBytes];
                    System.arraycopy(data, 0, this.receiveData, 0, numberOfBytes);
                    for (ReceiveDataChangedListener hl : this.receiveDataChangedListener) {
                        hl.ReceiveDataChanged();
                    }
                }
            }
            if (data[7] == 132 & data[8] == 1) {
                throw new FunctionCodeNotSupportedException("Function code not supported by master");
            }
            if (data[7] == 132 & data[8] == 2) {
                throw new StartingAddressInvalidException("Starting adress invalid or starting adress + quantity invalid");
            }
            if (data[7] == 132 & data[8] == 3) {
                throw new QuantityInvalidException("Quantity invalid");
            }
            if (data[7] == 132 & data[8] == 4) {
                throw new ModbusException("Error reading");
            }
            for (int i = 0; i < quantity; ++i) {
                byte[] bytes = new byte[]{data[9 + i * 2], data[9 + i * 2 + 1]};
                ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
                response[i] = byteBuffer.getShort();
            }
        }
        return response;
    }

    public void WriteSingleCoil(int startingAddress, boolean value) throws ModbusException, UnknownHostException, SocketException, IOException {
        if (this.tcpClientSocket == null & !this.udpFlag) {
            throw new ConnectionException("connection error");
        }
        byte[] coilValue = new byte[2];
        this.transactionIdentifier = ModbusClient.toByteArray(1);
        this.protocolIdentifier = ModbusClient.toByteArray(0);
        this.length = ModbusClient.toByteArray(6);
        this.unitIdentifier = 0;
        this.functionCode = 5;
        this.startingAddress = ModbusClient.toByteArray(startingAddress);
        coilValue = value ? ModbusClient.toByteArray(65280) : ModbusClient.toByteArray(0);
        byte[] data = new byte[]{this.transactionIdentifier[1], this.transactionIdentifier[0], this.protocolIdentifier[1], this.protocolIdentifier[0], this.length[1], this.length[0], this.unitIdentifier, this.functionCode, this.startingAddress[1], this.startingAddress[0], coilValue[1], coilValue[0], this.crc[0], this.crc[1]};
        if (this.tcpClientSocket.isConnected() | this.udpFlag) {
            if (this.udpFlag) {
                InetAddress ipAddress = InetAddress.getByName(this.ipAddress);
                DatagramPacket sendPacket = new DatagramPacket(data, data.length, ipAddress, this.port);
                DatagramSocket clientSocket = new DatagramSocket();
                clientSocket.setSoTimeout(500);
                clientSocket.send(sendPacket);
                data = new byte[2100];
                DatagramPacket receivePacket = new DatagramPacket(data, data.length);
                clientSocket.receive(receivePacket);
                data = receivePacket.getData();
            } else {
                this.outStream.write(data, 0, data.length - 2);
                if (this.sendDataChangedListener.size() > 0) {
                    this.sendData = new byte[data.length - 2];
                    System.arraycopy(data, 0, this.sendData, 0, data.length - 2);
                    for (SendDataChangedListener hl : this.sendDataChangedListener) {
                        hl.SendDataChanged();
                    }
                }
                data = new byte[2100];
                int numberOfBytes = this.inStream.read(data, 0, data.length);
                if (this.receiveDataChangedListener.size() > 0) {
                    this.receiveData = new byte[numberOfBytes];
                    System.arraycopy(data, 0, this.receiveData, 0, numberOfBytes);
                    for (ReceiveDataChangedListener hl : this.receiveDataChangedListener) {
                        hl.ReceiveDataChanged();
                    }
                }
            }
        }
        if (data[7] == 133 & data[8] == 1) {
            throw new FunctionCodeNotSupportedException("Function code not supported by master");
        }
        if (data[7] == 133 & data[8] == 2) {
            throw new StartingAddressInvalidException("Starting address invalid or starting address + quantity invalid");
        }
        if (data[7] == 133 & data[8] == 3) {
            throw new QuantityInvalidException("quantity invalid");
        }
        if (data[7] == 133 & data[8] == 4) {
            throw new ModbusException("error reading");
        }
    }

    public void WriteSingleRegister(int startingAddress, int value) throws ModbusException, UnknownHostException, SocketException, IOException {
        if (this.tcpClientSocket == null & !this.udpFlag) {
            throw new ConnectionException("connection error");
        }
        byte[] registerValue = new byte[2];
        this.transactionIdentifier = ModbusClient.toByteArray(1);
        this.protocolIdentifier = ModbusClient.toByteArray(0);
        this.length = ModbusClient.toByteArray(6);
        this.functionCode = 6;
        this.startingAddress = ModbusClient.toByteArray(startingAddress);
        registerValue = ModbusClient.toByteArray((short)value);
        byte[] data = new byte[]{this.transactionIdentifier[1], this.transactionIdentifier[0], this.protocolIdentifier[1], this.protocolIdentifier[0], this.length[1], this.length[0], this.unitIdentifier, this.functionCode, this.startingAddress[1], this.startingAddress[0], registerValue[1], registerValue[0], this.crc[0], this.crc[1]};
        if (this.udpFlag) {
            InetAddress ipAddress = InetAddress.getByName(this.ipAddress);
            DatagramPacket sendPacket = new DatagramPacket(data, data.length, ipAddress, this.port);
            DatagramSocket clientSocket = new DatagramSocket();
            clientSocket.setSoTimeout(500);
            clientSocket.send(sendPacket);
            data = new byte[2100];
            DatagramPacket receivePacket = new DatagramPacket(data, data.length);
            clientSocket.receive(receivePacket);
            data = receivePacket.getData();
        } else {
            this.outStream.write(data, 0, data.length - 2);
            if (this.sendDataChangedListener.size() > 0) {
                this.sendData = new byte[data.length - 2];
                System.arraycopy(data, 0, this.sendData, 0, data.length - 2);
                for (SendDataChangedListener hl : this.sendDataChangedListener) {
                    hl.SendDataChanged();
                }
            }
            data = new byte[2100];
            int numberOfBytes = this.inStream.read(data, 0, data.length);
            if (this.receiveDataChangedListener.size() > 0) {
                this.receiveData = new byte[numberOfBytes];
                System.arraycopy(data, 0, this.receiveData, 0, numberOfBytes);
                for (ReceiveDataChangedListener hl : this.receiveDataChangedListener) {
                    hl.ReceiveDataChanged();
                }
            }
        }
        if (data[7] == 134 & data[8] == 1) {
            throw new FunctionCodeNotSupportedException("Function code not supported by master");
        }
        if (data[7] == 134 & data[8] == 2) {
            throw new StartingAddressInvalidException("Starting address invalid or starting address + quantity invalid");
        }
        if (data[7] == 134 & data[8] == 3) {
            throw new QuantityInvalidException("quantity invalid");
        }
        if (data[7] == 134 & data[8] == 4) {
            throw new ModbusException("error reading");
        }
    }

    public void WriteMultipleCoils(int startingAddress, boolean[] values) throws ModbusException, UnknownHostException, SocketException, IOException {
        byte byteCount = (byte)(values.length / 8 + 1);
        byte[] quantityOfOutputs = ModbusClient.toByteArray(values.length);
        int singleCoilValue = 0;
        if (this.tcpClientSocket == null & !this.udpFlag) {
            throw new ConnectionException("connection error");
        }
        this.transactionIdentifier = ModbusClient.toByteArray(1);
        this.protocolIdentifier = ModbusClient.toByteArray(0);
        this.length = ModbusClient.toByteArray(7 + (values.length / 8 + 1));
        this.functionCode = 15;
        this.startingAddress = ModbusClient.toByteArray(startingAddress);
        byte[] data = new byte[16 + values.length / 8];
        data[0] = this.transactionIdentifier[1];
        data[1] = this.transactionIdentifier[0];
        data[2] = this.protocolIdentifier[1];
        data[3] = this.protocolIdentifier[0];
        data[4] = this.length[1];
        data[5] = this.length[0];
        data[6] = this.unitIdentifier;
        data[7] = this.functionCode;
        data[8] = this.startingAddress[1];
        data[9] = this.startingAddress[0];
        data[10] = quantityOfOutputs[1];
        data[11] = quantityOfOutputs[0];
        data[12] = byteCount;
        for (int i = 0; i < values.length; ++i) {
            if (i % 8 == 0) {
                singleCoilValue = 0;
            }
            int CoilValue = values[i] ? 1 : 0;
            data[13 + i / 8] = singleCoilValue = (int)((byte)(CoilValue << i % 8 | singleCoilValue));
        }
        data[data.length - 2] = this.crc[0];
        data[data.length - 1] = this.crc[1];
        if (this.udpFlag) {
            InetAddress ipAddress = InetAddress.getByName(this.ipAddress);
            DatagramPacket sendPacket = new DatagramPacket(data, data.length, ipAddress, this.port);
            DatagramSocket clientSocket = new DatagramSocket();
            clientSocket.setSoTimeout(500);
            clientSocket.send(sendPacket);
            data = new byte[2100];
            DatagramPacket receivePacket = new DatagramPacket(data, data.length);
            clientSocket.receive(receivePacket);
            data = receivePacket.getData();
        } else {
            this.outStream.write(data, 0, data.length - 2);
            if (this.sendDataChangedListener.size() > 0) {
                this.sendData = new byte[data.length - 2];
                System.arraycopy(data, 0, this.sendData, 0, data.length - 2);
                for (SendDataChangedListener hl : this.sendDataChangedListener) {
                    hl.SendDataChanged();
                }
            }
            data = new byte[2100];
            int numberOfBytes = this.inStream.read(data, 0, data.length);
            if (this.receiveDataChangedListener.size() > 0) {
                this.receiveData = new byte[numberOfBytes];
                System.arraycopy(data, 0, this.receiveData, 0, numberOfBytes);
                for (ReceiveDataChangedListener hl : this.receiveDataChangedListener) {
                    hl.ReceiveDataChanged();
                }
            }
        }
        if (data[7] == 143 & data[8] == 1) {
            throw new FunctionCodeNotSupportedException("Function code not supported by master");
        }
        if (data[7] == 143 & data[8] == 2) {
            throw new StartingAddressInvalidException("Starting address invalid or starting address + quantity invalid");
        }
        if (data[7] == 143 & data[8] == 3) {
            throw new QuantityInvalidException("quantity invalid");
        }
        if (data[7] == 143 & data[8] == 4) {
            throw new ModbusException("error reading");
        }
    }

    public void WriteMultipleRegisters(int startingAddress, int[] values) throws ModbusException, UnknownHostException, SocketException, IOException {
        byte byteCount = (byte)(values.length * 2);
        byte[] quantityOfOutputs = ModbusClient.toByteArray(values.length);
        if (this.tcpClientSocket == null & !this.udpFlag) {
            throw new ConnectionException("connection error");
        }
        this.transactionIdentifier = ModbusClient.toByteArray(1);
        this.protocolIdentifier = ModbusClient.toByteArray(0);
        this.length = ModbusClient.toByteArray(7 + values.length * 2);
        this.functionCode = 16;
        this.startingAddress = ModbusClient.toByteArray(startingAddress);
        byte[] data = new byte[15 + values.length * 2];
        data[0] = this.transactionIdentifier[1];
        data[1] = this.transactionIdentifier[0];
        data[2] = this.protocolIdentifier[1];
        data[3] = this.protocolIdentifier[0];
        data[4] = this.length[1];
        data[5] = this.length[0];
        data[6] = this.unitIdentifier;
        data[7] = this.functionCode;
        data[8] = this.startingAddress[1];
        data[9] = this.startingAddress[0];
        data[10] = quantityOfOutputs[1];
        data[11] = quantityOfOutputs[0];
        data[12] = byteCount;
        for (int i = 0; i < values.length; ++i) {
            byte[] singleRegisterValue = ModbusClient.toByteArray(values[i]);
            data[13 + i * 2] = singleRegisterValue[1];
            data[14 + i * 2] = singleRegisterValue[0];
        }
        data[data.length - 2] = this.crc[0];
        data[data.length - 1] = this.crc[1];
        if (this.udpFlag) {
            InetAddress ipAddress = InetAddress.getByName(this.ipAddress);
            DatagramPacket sendPacket = new DatagramPacket(data, data.length, ipAddress, this.port);
            DatagramSocket clientSocket = new DatagramSocket();
            clientSocket.setSoTimeout(500);
            clientSocket.send(sendPacket);
            data = new byte[2100];
            DatagramPacket receivePacket = new DatagramPacket(data, data.length);
            clientSocket.receive(receivePacket);
            data = receivePacket.getData();
        } else {
            this.outStream.write(data, 0, data.length - 2);
            if (this.sendDataChangedListener.size() > 0) {
                this.sendData = new byte[data.length - 2];
                System.arraycopy(data, 0, this.sendData, 0, data.length - 2);
                for (SendDataChangedListener hl : this.sendDataChangedListener) {
                    hl.SendDataChanged();
                }
            }
            data = new byte[2100];
            int numberOfBytes = this.inStream.read(data, 0, data.length);
            if (this.receiveDataChangedListener.size() > 0) {
                this.receiveData = new byte[numberOfBytes];
                System.arraycopy(data, 0, this.receiveData, 0, numberOfBytes);
                for (ReceiveDataChangedListener hl : this.receiveDataChangedListener) {
                    hl.ReceiveDataChanged();
                }
            }
        }
        if (data[7] == 144 & data[8] == 1) {
            throw new FunctionCodeNotSupportedException("Function code not supported by master");
        }
        if (data[7] == 144 & data[8] == 2) {
            throw new StartingAddressInvalidException("Starting address invalid or starting address + quantity invalid");
        }
        if (data[7] == 144 & data[8] == 3) {
            throw new QuantityInvalidException("quantity invalid");
        }
        if (data[7] == 144 & data[8] == 4) {
            throw new ModbusException("error reading");
        }
    }

    public int[] ReadWriteMultipleRegisters(int startingAddressRead, int quantityRead, int startingAddressWrite, int[] values) throws ModbusException, UnknownHostException, SocketException, IOException {
        byte[] startingAddressReadLocal = new byte[2];
        byte[] quantityReadLocal = new byte[2];
        byte[] startingAddressWriteLocal = new byte[2];
        byte[] quantityWriteLocal = new byte[2];
        byte writeByteCountLocal = 0;
        if (this.tcpClientSocket == null & !this.udpFlag) {
            throw new ConnectionException("connection error");
        }
        if (startingAddressRead > 65535 | quantityRead > 125 | startingAddressWrite > 65535 | values.length > 121) {
            throw new IllegalArgumentException("Starting address must be 0 - 65535; quantity must be 0 - 125");
        }
        this.transactionIdentifier = ModbusClient.toByteArray(1);
        this.protocolIdentifier = ModbusClient.toByteArray(0);
        this.length = ModbusClient.toByteArray(6);
        this.functionCode = 23;
        startingAddressReadLocal = ModbusClient.toByteArray(startingAddressRead);
        quantityReadLocal = ModbusClient.toByteArray(quantityRead);
        startingAddressWriteLocal = ModbusClient.toByteArray(startingAddressWrite);
        quantityWriteLocal = ModbusClient.toByteArray(values.length);
        writeByteCountLocal = (byte)(values.length * 2);
        byte[] data = new byte[19 + values.length * 2];
        data[0] = this.transactionIdentifier[1];
        data[1] = this.transactionIdentifier[0];
        data[2] = this.protocolIdentifier[1];
        data[3] = this.protocolIdentifier[0];
        data[4] = this.length[1];
        data[5] = this.length[0];
        data[6] = this.unitIdentifier;
        data[7] = this.functionCode;
        data[8] = startingAddressReadLocal[1];
        data[9] = startingAddressReadLocal[0];
        data[10] = quantityReadLocal[1];
        data[11] = quantityReadLocal[0];
        data[12] = startingAddressWriteLocal[1];
        data[13] = startingAddressWriteLocal[0];
        data[14] = quantityWriteLocal[1];
        data[15] = quantityWriteLocal[0];
        data[16] = writeByteCountLocal;
        for (int i = 0; i < values.length; ++i) {
            byte[] singleRegisterValue = ModbusClient.toByteArray(values[i]);
            data[17 + i * 2] = singleRegisterValue[1];
            data[18 + i * 2] = singleRegisterValue[0];
        }
        data[data.length - 2] = this.crc[0];
        data[data.length - 1] = this.crc[1];
        if (this.udpFlag) {
            InetAddress ipAddress = InetAddress.getByName(this.ipAddress);
            DatagramPacket sendPacket = new DatagramPacket(data, data.length, ipAddress, this.port);
            DatagramSocket clientSocket = new DatagramSocket();
            clientSocket.setSoTimeout(500);
            clientSocket.send(sendPacket);
            data = new byte[2100];
            DatagramPacket receivePacket = new DatagramPacket(data, data.length);
            clientSocket.receive(receivePacket);
            data = receivePacket.getData();
        } else {
            this.outStream.write(data, 0, data.length - 2);
            if (this.sendDataChangedListener.size() > 0) {
                this.sendData = new byte[data.length - 2];
                System.arraycopy(data, 0, this.sendData, 0, data.length - 2);
                for (SendDataChangedListener hl : this.sendDataChangedListener) {
                    hl.SendDataChanged();
                }
            }
            data = new byte[2100];
            int numberOfBytes = this.inStream.read(data, 0, data.length);
            if (this.receiveDataChangedListener.size() > 0) {
                this.receiveData = new byte[numberOfBytes];
                System.arraycopy(data, 0, this.receiveData, 0, numberOfBytes);
                for (ReceiveDataChangedListener hl : this.receiveDataChangedListener) {
                    hl.ReceiveDataChanged();
                }
            }
        }
        if (data[7] == 151 & data[8] == 1) {
            throw new FunctionCodeNotSupportedException("Function code not supported by master");
        }
        if (data[7] == 151 & data[8] == 2) {
            throw new StartingAddressInvalidException("Starting address invalid or starting address + quantity invalid");
        }
        if (data[7] == 151 & data[8] == 3) {
            throw new QuantityInvalidException("quantity invalid");
        }
        if (data[7] == 151 & data[8] == 4) {
            throw new ModbusException("error reading");
        }
        int[] response = new int[quantityRead];
        for (int i2 = 0; i2 < quantityRead; ++i2) {
            byte highByte = data[9 + i2 * 2];
            byte lowByte = data[9 + i2 * 2 + 1];
            byte[] bytes = new byte[]{highByte, lowByte};
            ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
            response[i2] = byteBuffer.getShort();
        }
        return response;
    }

    public void Disconnect() throws IOException {
        this.inStream.close();
        this.outStream.close();
        this.tcpClientSocket.close();
    }

    public static byte[] toByteArray(int value) {
        byte[] result = new byte[2];
        result[1] = (byte)(value >> 8);
        result[0] = (byte)value;
        return result;
    }

    public static byte[] toByteArrayDouble(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    public static byte[] toByteArray(float value) {
        return ByteBuffer.allocate(4).putFloat(value).array();
    }

    public boolean isConnected() {
        boolean returnValue = false;
        returnValue = this.tcpClientSocket == null ? false : this.tcpClientSocket.isConnected();
        return returnValue;
    }

    public String getipAddress() {
        return this.ipAddress;
    }

    public void setipAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean getUDPFlag() {
        return this.udpFlag;
    }

    public void setUDPFlag(boolean udpFlag) {
        this.udpFlag = udpFlag;
    }

    public int getConnectionTimeout() {
        return this.connectTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectTimeout = connectionTimeout;
    }

    public void addReveiveDataChangedListener(ReceiveDataChangedListener toAdd) {
        this.receiveDataChangedListener.add(toAdd);
    }

    public void addSendDataChangedListener(SendDataChangedListener toAdd) {
        this.sendDataChangedListener.add(toAdd);
    }

    public static enum RegisterOrder {
        LowHigh,
        HighLow;
        

        private RegisterOrder() {
        }
    }

}

