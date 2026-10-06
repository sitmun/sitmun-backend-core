package org.sitmun.administration.service.access;

import java.util.concurrent.Semaphore;

public final class ServiceCheckInFlight {

  private final Semaphore permits;

  public ServiceCheckInFlight(int permits) {
    this.permits = new Semaphore(permits, true);
  }

  public void acquire() throws InterruptedException {
    permits.acquire();
  }

  public boolean tryAcquire() {
    return permits.tryAcquire();
  }

  public int availablePermits() {
    return permits.availablePermits();
  }

  public void release() {
    permits.release();
  }
}
