/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */
/*
logger.INFO("Test")

function registerCommand(name, description, callback) {
    var cmd = {

        execute : callback,

        getName : function() {
            return name;
        },

        getDescription : function() {
            return description;
        }

    }

    scriptEngine.register(name, cmd, 'sirius.web.health.console.Command');
}

function registerService(name, callback) {
    var serv = {
        call : callback
    }

    scriptEngine.register(name, serv, 'sirius.web.services.StructuredService');
}

registerService('test', function(c, o) {
    o.beginResult('a');
    o.property('test','111a');
    o.endResult();
});
*/