/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.blink;

import java.io.IOException;

import org.apache.hadoop.hdfs.server.namenode.NotReplicatedYetException;
import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.FBTNodeVisitor;
import org.apache.hadoop.hdfs.server.namenodeFBT.Node;
import org.apache.hadoop.hdfs.server.namenodeFBT.Request;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;

/**
 * @author hanhlh
 *
 */
public abstract class FBLTNodeVisitor extends FBTNodeVisitor {

	public FBLTNodeVisitor(FBTDirectory directory) {
		super(directory);
	}

	public void setRequest(Request request) {
        super.setRequest(request);
    }
	public void run() {
		StringUtility.debugSpace("FBLTNodeVisitor.run");
		// TODO 自動生成されたメソッド・スタブ
		VPointer vp = _request.getTarget();

        if (vp == null) {
            /* ルートから処理を開始 */
            try {
				_directory.accept(this);
			} catch (NotReplicatedYetException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			} catch (MessageException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			} catch (IOException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
        } else {
            /*
             * 他の PE から転送されてきた要求の場合は vp != null となる
             * vp で指定されたノードへ Visitor を渡す
             */
        	System.out.println("vp "+vp.toString());
            Node node = _directory.getNode(vp);
            try {
				node.accept(this, vp);
			} catch (NotReplicatedYetException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			} catch (MessageException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			} catch (IOException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
        }
	}
	public void lock(VPointer target, int mode, int h) {
//      super.lock(target, mode + 10 * h);
      lock(target, mode);
  }

  public void lock(VPointer target, int mode, int h, int c, int r) {
      lock(target, mode);
      _locker.incrementXCount(h, c, r);
  }

  protected FBTDirectory getDirectory() {
      return (FBTDirectory) _directory;
  }

}
