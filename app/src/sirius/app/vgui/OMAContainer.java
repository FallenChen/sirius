package sirius.app.vgui;

import com.google.common.collect.Lists;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.MethodProperty;
import sirius.app.frameworks.users.User;
import sirius.app.oma.Entity;
import sirius.app.oma.OMA;
import sirius.app.oma.query.Query;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 20.01.14
 * Time: 22:32
 * To change this template use File | Settings | File Templates.
 */
public class OMAContainer implements Container, Container.Indexed {

    private final List<String> propertyIds = new ArrayList<String>();
    private final Map<String, Class<?>> propertyTypes = new HashMap<String, Class<?>>();
    private final Map<Object, Entity> cache = new HashMap<Object, Entity>();

    private Query<User> query = OMA.select(User.class);

    public OMAContainer() {
        propertyIds.add(User.LOGIN_NAME);
        propertyTypes.put(User.LOGIN_NAME, String.class);
    }

    @Override
    public int indexOfId(Object itemId) {
        return 0;
    }

    @Override
    public Object getIdByIndex(int index) {
        return null;
    }

    @Override
    public List<?> getItemIds(int startIndex, int numberOfItems) {
        List<Object> result = Lists.newArrayList();
        for (Entity e : query.limit(startIndex, numberOfItems).list()) {
            result.add(e.getId());
            cache.put(e.getId(), e);
        }
        return result;
    }

    @Override
    public Object addItemAt(int index) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Item addItemAt(int index, Object newItemId) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object nextItemId(Object itemId) {
        return null;
    }

    @Override
    public Object prevItemId(Object itemId) {
        return null;
    }

    @Override
    public Object firstItemId() {
        return null;
    }

    @Override
    public Object lastItemId() {
        return null;
    }

    @Override
    public boolean isFirstId(Object itemId) {
        return false;
    }

    @Override
    public boolean isLastId(Object itemId) {
        return false;
    }

    @Override
    public Object addItemAfter(Object previousItemId) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Item addItemAfter(Object previousItemId, Object newItemId) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Item getItem(Object itemId) {
        return new BeanItem<Object>(cache.get(itemId));
    }

    @Override
    public Collection<?> getContainerPropertyIds() {
        return propertyIds;
    }

    @Override
    public Collection<?> getItemIds() {
        return null;
    }

    @Override
    public Property getContainerProperty(Object itemId, Object propertyId) {
        return new MethodProperty(cache.get(itemId),(String)propertyId);
//        return null;
    }

    @Override
    public Class<?> getType(Object propertyId) {
        return propertyTypes.get(propertyId);
    }

    @Override
    public int size() {
        return query.count();
    }

    @Override
    public boolean containsId(Object itemId) {
        return cache.containsKey(itemId);
    }

    @Override
    public Item addItem(Object itemId) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object addItem() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeItem(Object itemId) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addContainerProperty(Object propertyId,
                                        Class<?> type,
                                        Object defaultValue) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeContainerProperty(Object propertyId) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAllItems() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}
