package sirius.kernel.references;

public class IndexValueReference<E> extends ReadonlyValueReference<E[], E> {

    private int index;

    public IndexValueReference(int index) {
        super();
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public E getValue(E[] data) {
        return data[index];
    }

}
