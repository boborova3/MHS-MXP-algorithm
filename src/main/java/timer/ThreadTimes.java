package timer;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ThreadTimes extends Thread {

    private static final long BILLION = 1000000000;
    private final long interval;
    private final long threadId;
    private final Map<Long, Times> history = new ConcurrentHashMap<>(8, 0.9f, 1);

    /**
     * Create a polling thread to track times.
     */
    public ThreadTimes(final long interval) {
        super("Thread time monitor");
        this.interval = interval;
        threadId = getId();
        setDaemon(true);
    }

    /**
     * Run the thread until interrupted.
     */
    public void run() {
        while (!isInterrupted()) {
            update();
            try {
                sleep(interval);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    /**
     * Update the hash table of thread times.
     */
    private void update() {
        final ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        final long[] ids = bean.getAllThreadIds();
        for (long id : ids) {

            if (id == threadId) {
                continue; // Exclude polling thread
            }

            final long cpuTime = bean.getThreadCpuTime(id);
            final long userTime = bean.getThreadUserTime(id);

            if (cpuTime == -1 || userTime == -1) {
                continue; // Thread died
            }

            Times times = history.get(id);

            if (times == null) {
                times = new Times();
                times.id = id;
                times.startCpuTime = cpuTime;
                times.startUserTime = userTime;
                times.endCpuTime = cpuTime;
                times.endUserTime = userTime;
                history.put(id, times);
            } else {
                times.endCpuTime = cpuTime;
                times.endUserTime = userTime;
            }
        }
    }

    /**
     * Get total CPU time so far in nanoseconds.
     */
    public long getTotalCpuTime() {
        final Collection<Times> hist = history.values();
        long time = 0L;

        for (Times times : hist) {
            time += (times.endCpuTime - times.startCpuTime);
        }

        return time;
    }

    public double getTotalCpuTimeInSec() {
        return (double) getTotalCpuTime() / BILLION;
    }

    /**
     * Get total user time so far in nanoseconds.
     */
    public long getTotalUserTime() {
        final Collection<Times> hist = history.values();
        long time = 0L;

        for (Times times : hist) {
            time += (times.endUserTime - times.startUserTime);
        }

        return time;
    }

    public double getTotalUserTimeInSec() {
        return (double) getTotalUserTime() / BILLION;
    }

    /**
     * Get total system time so far in nanoseconds.
     */
    public long getTotalSystemTime() {
        return getTotalCpuTime() - getTotalUserTime();
    }

    public double getTotalSystemTimeInSec() {
        return (double) getTotalSystemTime() / BILLION;
    }

    private class Times {
        long id;
        long startCpuTime;
        long startUserTime;
        long endCpuTime;
        long endUserTime;
    }
}
