public class SyncCounter {
    private int count;
    public SyncCounter(int count){
        this.count = count;
    }
    public synchronized void decrease(){
        count--;
    }
    public synchronized int getCount(){
        return count;
    }
}
