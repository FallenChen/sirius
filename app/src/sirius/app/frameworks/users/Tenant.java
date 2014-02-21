package sirius.app.frameworks.users;

import sirius.app.oma.Entity;
import sirius.app.oma.Many;
import sirius.app.oma.annotations.Length;
import sirius.app.oma.annotations.NotNull;
import sirius.app.oma.annotations.RefType;
import sirius.app.oma.annotations.Table;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 11.01.14
 * Time: 20:14
 * To change this template use File | Settings | File Templates.
 */
@Table
public class Tenant extends Entity {

    @NotNull
    @Length(100)
    private String name;

    @RefType(value=User.class, field = User.TENANT)
    private Many<User> users;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Many<User> getUsers() {
        return users;
    }
}
