package org.apache.hadoop.hdfs.server.namenode.writeOffLoading;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.hadoop.hdfs.protocol.Block;

public class WriteOffLoadingCommand implements Serializable {

	private ArrayList<Block[]> _blocks;

	private String _wolDst;

	private String _destination;

	public WriteOffLoadingCommand (ArrayList<Block[]> blocks, String destination) {
		this(blocks, destination, null);
	}

	public WriteOffLoadingCommand (ArrayList<Block[]> blocks, String destination,
				String wolDst) {
		_blocks = blocks;
		_destination = destination;
		_wolDst = wolDst;
	}

	public ArrayList<Block[]> getBlocks() {
		return _blocks;
	}


	public void setBlocks(ArrayList<Block[]> _blocks) {
		this._blocks = _blocks;
	}

	public String getDestination() {
		return _destination;
	}

	public void setDestination(String _destination) {
		this._destination = _destination;
	}

	public String getWOLDestination() {
		return _wolDst;
	}

	public Block[] getBlocksArray() {
		Block[] newBlocks = new Block[size()];
		System.out.println("size,"+size());
		int id=0;
		for (Block[] bs : _blocks) {
			for (Block b : bs) {
				newBlocks[id] = b;
				id++;
			}
		}
		return newBlocks;
	}
	public int size() {
		int size=0;
		for (Block[] bs:_blocks) {
			System.out.println("size,"+size);
			System.out.println("length,"+bs.length);
			size+=bs.length;
			System.out.println("size,"+size);
		}
		return size;

	}
	@Override
	public String toString() {
		return "WriteOffLoadingCommand [_blocks=" + Arrays.toString(getBlocksArray()) + ", _destination="
				+ _destination + "]";
	}





}
