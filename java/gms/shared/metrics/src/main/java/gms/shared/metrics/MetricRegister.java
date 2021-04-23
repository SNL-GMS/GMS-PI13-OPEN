package gms.shared.metrics;

import javax.management.*;
import java.lang.management.*;

public class MetricRegister {
    private static MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

    public static void register(CustomMetric customMetric, ObjectName name) throws JMException {
        mBeanServer.registerMBean(customMetric, name);
    }
}
