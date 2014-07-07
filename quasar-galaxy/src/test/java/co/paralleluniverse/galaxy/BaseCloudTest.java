package co.paralleluniverse.galaxy;

import org.gridkit.nanocloud.CloudFactory;

import org.gridkit.vicluster.ViManager;
import org.gridkit.vicluster.ViProps;
import org.junit.After;

public abstract class BaseCloudTest {

    protected ViManager cloud;

    @After
    public void recycleCloud() {
        if (cloud != null) {
            cloud.shutdown();
        }
    }

    static ViManager createLocalCloud() {
        ViManager vim = CloudFactory.createCloud();
        ViProps.at(vim.node("**")).setLocalType();
        return vim;
    }
}
