/**
 * @fileOverview Adds pieces to Exhibit.Expression to fit Backstage
 *     architecture.
 * @author David Huynh
 * @author <a href="mailto:ryanlee@zepheira.com">Ryan Lee</a>
 */

/**
 * @returns {Object}
 */
Exhibit.Expression._Impl.prototype.getServerSideConfiguration = function() {
    return {
        type:       "expression",
        rootNode:   this._rootNode.getServerSideConfiguration()
    };
};

/**
 * @returns {Object}
 */
Exhibit.Expression.Path.prototype.getServerSideConfiguration = function() {
    return {
        type:       "path",
        rootName:   this._rootName,
        segments:   this._segments
    };
};

/**
 * @returns {Object}
 */
Exhibit.Expression._Constant.prototype.getServerSideConfiguration = function() {
    return {
        type:       "constant",
        value:      this._value,
        valueType:  this._valueType
    };
};

/**
 * @returns {Object}
 */
Exhibit.Expression._Operator.prototype.getServerSideConfiguration = function() {
    var args, i;
    args = [];
    for (i = 0; i < this._args.length; i++) {
        args.push(this._args[i].getServerSideConfiguration());
    }
    return {
        type:       "operator",
        operator:   this._operator,
        args:       args
    };
};

/**
 * @returns {Object}
 */
Exhibit.Expression._FunctionCall.prototype.getServerSideConfiguration = function() {
    var args, i;
    args = [];
    for (i = 0; i < this._args.length; i++) {
        args.push(this._args[i].getServerSideConfiguration());
    }
    return {
        type:       "function-call",
        name:       this._name,
        args:       args
    };
};

/**
 * @returns {Object}
 */
Exhibit.Expression._ControlCall.prototype.getServerSideConfiguration = function() {
    var args, i;
    args = [];
    for (i = 0; i < this._args.length; i++) {
        args.push(this._args[i].getServerSideConfiguration());
    }
    return {
        type:       "control-call",
        name:       this._name,
        args:       args
    };
};
