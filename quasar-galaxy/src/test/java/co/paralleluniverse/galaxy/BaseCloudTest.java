package co.paralleluniverse.galaxy;



import java.lang.management.ManagementFactory;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.concurrent.Callable;

import org.gridkit.vicluster.ViManager;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViNodeSet;
import org.junit.After;

public abstract class BaseCloudTest {

	protected ViManager cloud;

	@After
	public void recycleCloud() {
		if (cloud != null) {
			cloud.shutdown();
		}
	}	
	
	protected void sayHelloWorld(ViNodeSet cloud) {

		// two starts will match any node name
		ViNode allNodes = cloud.node("**");
		
		allNodes.exec(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				String jvmName = ManagementFactory.getRuntimeMXBean().getName();
				System.out.println("My name is '" + jvmName + "'. Hello!");
				return null;
			}
		});
	}

	protected void reportMemory(ViNodeSet cloud) {
		
		// two starts will match any node name
		ViNode allNodes = cloud.node("**");
		
		allNodes.exec(new Callable<Void>() {
			
			@Override
			public Void call() throws Exception {
				String jvmName = ManagementFactory.getRuntimeMXBean().getName();
				long totalMemory = Runtime.getRuntime().maxMemory();
				System.out.println("My name is '" + jvmName + "'. Memory limit is " + (totalMemory >> 20) + "MiB");
				return null;
			}
		});
	}

	/**
	 * This method will force initialization of all declared nodes.
	 */
	protected void warmUp(ViNodeSet cloud) {
		// two starts will match any node name
		ViNode allNodes = cloud.node("**");
		
		// ViNode object may represent a single node or a group
		
		// ViNode.exec(...) call has blocking semantic so it will force all 
		// lazy initialization to finish and wait until runnable is executed 
		// on every node in group 
		allNodes.exec(new Runnable() {
			@Override
			public void run() {
				// do nothing
			}
		});
                
	}		
}
