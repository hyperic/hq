dojo.provide("tapestry.fx");
dojo.require("tapestry.core");

/**
 * package: tapestry.fx
 * Provides handling of effects applied before, during or after an XHR request/response.
 */
function isEmpty(obj){
    if(obj){
        var tmp = {};
        var count = 0;
        for(var x in obj){
            if(obj[x] && (!tmp[x])){
                count++;
                break;
            } 
        }
        return count == 0; // boolean
    }     
    return false;
}

function show(node){
    var node = dojo.byId(node);
    if(node.style.display == 'none'){
        node.style.display = '';
    }    
}

tapestry.fx={
    
    // property: preEffects
    // Contains a reference to all registered pre-effects, i.e. effects that are
    // executed before an XHR request.
    preEffects:{},
    // property: postEffects
    // Contains a reference to all registered post-effects, i.e. effects that are
    // executed when new content arrives through an XHR response.
    postEffects:{},
    // property: ajaxStatusAction
    ajaxStatusAction:'loading',
    
    /**
     * Function: attachPreEffect
     * Schedules the execution of an effect when the specified link
     * is clicked (and thus an XHR request begins).
     * 
     * See Also:
     *  <tapestry.fx.attachPostEffect>  
     *  <dojo.lfx.IAnimation> 
     *
     * Parameters:
     *  triggerId - The clientId of the DirectLink that triggers the effect.
     *  animationFunc - A function that returns the animation to execute. 
     *  async - Boolean for whether to execute the effect in parallel to the
     *          XHR request. Defaults to false, i.e. the XHR is blocked until
     *          the effect ends.
     * 
     * Note:
     *  Here's an example usage:
     *      tapestry.fx.attachPreEffect("DirectLink",
     *          function(){return dojo.lfx.wipeOut("entry", 800, dojo.lfx.easeDefault) });
     */   
    attachPreEffect:function(triggerId, animationFunc, async){
        if (isEmpty(this.preEffects))
            this._initPreEffects();
        this.preEffects[triggerId] = {async:async, animation:animationFunc};        
    },
    
    /**
     * Function: attachPostEffect
     * Schedules the execution of an effect when the specified content
     * is returned through an XHR response.
     * 
     * See Also:
     *  <tapestry.fx.attachPreEffect>  
     *  <dojo.lfx.IAnimation> 
     *
     * Parameters:
     *  updateId - The id of a dom node that (when updated) triggers the effect.
     *  animationFunc - A function that returns the animation to execute. 
     * 
     * Note:
     *  Here's an example usage:
     *      tapestry.fx.attachPostEffect("entry",
     *          function(){return dojo.lfx.wipeIn("entry", 1500, dojo.lfx.easeDefault) });
     */       
    attachPostEffect:function(updateId, animationFunc){
        if (isEmpty(this.postEffects))
            this._initPostEffects();        
        this.postEffects[updateId] = {animation:animationFunc};
    },
    
    /**
     * Function: removeAll
     * Removes all registered effects (preEffects and postEffects).
     */       
    removeAll:function(){
        this.preEffects={};
        this.postEffects={};
    },

    /**
     * Function: attachAjaxStatus
     * Allows specifying a dom node that will be shown or hidden while ajax requests
     * are in progress or have finished.
     * Alternatively, one can specify a custom
     * function which will get invoked when an ajax request starts or ends - the first
     * argument to that function will be a boolean corresponding to wheather the status
     * element should be showing or not.
     *
     * Parameters:
     *  a1 - The dom id to show - hide, or the function to invoke when ajax starts or ends.
     */
    attachAjaxStatus:function(a1){
        /*
        dojo.log.debug("Attaching ajax status listener");
        if (dojo.lang.isString(a1)) {
            tapestry.fx.ajaxStatusAction =
                function(bShow){if (bShow) dojo.html.show(a1); else dojo.html.hide(a1);};
        }
        else if (dojo.lang.isFunction(a1)) {
            tapestry.fx.ajaxStatusAction = a1;
        }
        else {
            dojo.log.warn("Argument to tapestry.fx.attachAjaxStatus should be either a string or a function");
            return;
        }
        dojo.event.connectOnce(dojo.io, "queueBind", tapestry.fx._processAjaxStatus);
        dojo.event.connectOnce(tapestry, "error", tapestry.fx._processAjaxStatus);
        dojo.event.connectOnce(tapestry, "load", tapestry.fx._processAjaxStatus);
        dojo.event.connectOnce(tapestry, "loadJson", tapestry.fx._processAjaxStatus);
        */
    },

    _processAjaxStatus:function(){
        tapestry.fx.ajaxStatusAction.apply(this, [tapestry.isServingRequests()]);
    },
    
    _initPreEffects:function(){
        dojo.connect(tapestry, "linkOnClick", tapestry.fx, "_applyPreEffects");
    },
    
    _initPostEffects:function(){
        dojo.connect(tapestry, "loadContent", tapestry.fx, "_applyPostEffects");
    },
    
    _applyPreEffects:function(miObj){
        var id = miObj.args[1];        
        var effect = this.preEffects[id];
        if (effect){
                       
            var anim = effect.animation();
            
            if (effect.async){          
                anim.play();
                return miObj.proceed();                
            }
            else{
                anim.connect("onEnd", function(){ miObj.proceed(); });
                anim.play();
                return false;
            }
        }
        else{
            return miObj.proceed();
        }        
    },
    
    _applyPostEffects:function(miObj){
        var id = miObj.args[0];
        var effect = this.postEffects[id];
        if (effect){
            
            var ret = miObj.proceed();
            
            var anim = effect.animation();
            anim.play();
            
            return ret;
        }
        else{            
            return miObj.proceed();
        }        
    }
}
