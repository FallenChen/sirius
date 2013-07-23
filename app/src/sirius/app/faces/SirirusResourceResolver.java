package sirius.app.faces;

import com.sun.faces.facelets.impl.DefaultResourceResolver;

import java.net.URL;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 17.07.13
 * Time: 21:38
 * To change this template use File | Settings | File Templates.
 */
public class SirirusResourceResolver extends DefaultResourceResolver {

    @Override
    public URL resolveUrl(String resource) {
        if (resource != null && !"/".equals(resource)) {
            if (resource.startsWith("/view")) {
                URL url = getClass().getResource(resource);
                if (url != null) {
                    return url;
                }
            }
//            if (resource.startsWith("/artifacts")) {
//                URL url = Model.getModel().getPart(ArtifactManager.class).getArtifact(resource.substring(11), null);
//                if (url != null) {
//                    if (OCMWEB.LOG.isFine()) {
//                        OCMWEB.LOG.FINE(MessageFormat.format("Resolved: {0} to {1}", resource, url.toString()));
//                    }
//                    return url;
//                }
//            }
//            for (ResourceResolver res : resolvers.getParts()) {
//                URL result = res.resolveUrl(resource);
//                if (result != null) {
//                    return result;
//                }
//            }
        }

        // Use conventional lookup...
        return super.resolveUrl(resource);
    }


}
