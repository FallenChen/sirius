package sirius.web.health;

import com.google.common.collect.Maps;
import org.hyperic.sigar.*;
import sirius.kernel.async.CallContext;
import sirius.kernel.commons.Amount;
import sirius.kernel.commons.Collector;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Log;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/09
 */
@Register
public class SystemMetricProvider implements MetricProvider {
    private Sigar sigar = new Sigar();
    protected static final Log LOG = Log.get("sigar");
    private Map<String, Double> rxMap = Maps.newTreeMap();
    private Map<String, Double> txMap = Maps.newTreeMap();
    private long lastInteractionCounter = 0;

    @Override
    public void gather(Collector<Metric> collector) {
        gatherInteractions(collector);
//        try {
////            gartherCPUandMem(collector);
////            gatherNetworkStats(collector);
//        } catch (SigarException e) {
//            Exceptions.handle(LOG, e);
//        }
    }

    private void gatherNetworkStats(Collector<Metric> collector) throws SigarException {
        for (String eth : sigar.getNetInterfaceList()) {
            NetInterfaceStat stat = sigar.getNetInterfaceStat(eth);
            double tx = stat.getTxBytes();
            Double lastTx = txMap.get(eth);
            if (lastTx != null) {
                collector.add(new Metric("System",
                                         eth + "-tx",
                                         Amount.of((tx - lastTx) / 10d).toScientificString(0, "b"),
                                         Metrics.MetricState.GREEN));
            }
            txMap.put(eth, tx);
            double rx = stat.getRxBytes();
            Double lastRx = rxMap.get(eth);
            if (lastRx != null) {
                collector.add(new Metric("System",
                                         eth + "-rx",
                                         Amount.of((rx - lastRx) / 10d).toScientificString(0, "b"),
                                         Metrics.MetricState.GREEN));
            }
            rxMap.put(eth, rx);
        }
    }

    private void gartherCPUandMem(Collector<Metric> collector) throws SigarException {
        CpuPerc cpu = sigar.getCpuPerc();
        collector.add(new Metric(Metrics.MAIN_CATEGORY,
                                 "CPU",
                                 Amount.of(cpu.getCombined()).toPercentString(),
                                 cpu.getCombined() < 50 ? Metrics.MetricState.GREEN : Metrics.MetricState.YELLOW));
        Mem mem = sigar.getMem();
        mem.gather(sigar);
        collector.add(new Metric("System",
                                 "Memory",
                                 Amount.of(mem.getUsedPercent()).toPercentString(),
                                 mem.getUsedPercent() < 80 ? Metrics.MetricState.GREEN : Metrics.MetricState.YELLOW));
    }

    private void gatherInteractions(Collector<Metric> collector) {
        long interactionCounter = CallContext.getInteractionCounter().getCount();
        if (interactionCounter > lastInteractionCounter) {
            collector.add(new Metric(Metrics.MAIN_CATEGORY,
                                     "Interactions",
                                     Amount.of((interactionCounter - lastInteractionCounter) / 10d)
                                           .toScientificString(0, null),
                                     Metrics.MetricState.GREEN));
        }
        lastInteractionCounter = interactionCounter;
    }
}
