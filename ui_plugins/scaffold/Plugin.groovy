import org.hyperic.hq.hqu.rendit.HQUPlugin

class Plugin extends HQUPlugin {
    Plugin() {
        /*
         * Uncomment the following to add a link to the plugin in the
         * Administration section.class
         *
         * The params are:
         *   autoAttach:  Attach this view automatically
         *   path:        The internal request to make to render the view
         *                (translates to http://localhost:7080/hqu/console/index.hqu)
         *                This should be a full request to a controller in /app
         *                ex:  If /app/BankController.groovy had a method 'debit'
         *                    '/bank/debit.hqu'
         *    
         *   name:        A description of the view, only used in the database.
         */
        // addAdminView(true, '/console/index.hqu', 'Groovy Console')
    }
}
