### control-flow helpers.

 Signals are implemented with launch()/finalize().  A signal id is just a
 string, so libraries can define their own exceptions without changing the
 runtime.
####

# Raise a control-flow signal. #
throw = (err) -> launch("error", err);

# try : body :: collector is useful when composing a protected operation. #
try = : body :: collector -> {
    err = finalize("error", body());
    if(err != null): {
        collector(err);
    };
};

# Assert a condition, otherwise raise an error. #
assert = (condition, message) -> {
	if(!condition) : {
		throw(message);
	};
};

### try usage: 
 try: {
     throw("err");
 } :: (err) -> {
    println(err);
 };
###

Exports = {};
Exports.throw = throw;
Exports.try = try;
Exports.assert = assert;

export(Exports);
