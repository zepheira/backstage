/*======================================================================
 *  Expression
 *======================================================================
 */
Exhibit.Expression._Impl.prototype.getServerSideConfiguration = function() {
    return {
        type:       "expression",
        rootNode:   this._rootNode.getServerSideConfiguration()
    };
};

Exhibit.Expression.Path.prototype.getServerSideConfiguration = function() {
    return {
        type:       "path",
        rootName:   this._rootName,
        segments:   this._segments
    };
};

Exhibit.Expression._Constant.prototype.getServerSideConfiguration = function() {
    return {
        type:       "constant",
        value:      this._value,
        valueType:  this._valueType
    };
};

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
