public class Counter {
    private int count = 0;
    synchronized public void inc(){
        count++;
    }
    public int getVal(){
        return this.count;
    }
}
