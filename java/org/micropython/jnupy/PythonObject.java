/*
 * This file is part of the Micro Python project, http://micropython.org/
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 EcmaXp
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.micropython.jnupy;

public class PythonObject {
    PythonState pythonState;
    long mpObject;

    PythonObject(PythonState pyState, long mpStateId, long objectId) {
        if (pyState == null || !pyState.checkState(mpStateId)) {
            throw new IllegalStateException("Python state is invaild.");
        }
        
        pythonState = pyState;
        mpObject = objectId;
        refIncr();
    }
    
    PythonObject(PythonState pyState, long objectId) {
        pyState.check();
        
        pythonState = pyState;
        mpObject = objectId;
        refIncr();
    }
    
    protected void finalize() throws Throwable {
        refDerc();
    }
    
    void refIncr() {
        pythonState.jnupy_ref_incr(this);
    }
    
    void refDerc() {
        pythonState.jnupy_ref_derc(this);        
    }
    
    public static PythonObject fromObject(Object obj) {
        if (obj == null) {
            throw new NullPointerException();
        } else if (obj instanceof PythonObject) {
			return (PythonObject)obj;
		}
		
		// TODO: change excpetion
		throw new RuntimeException("invaild python raw object: " + obj.toString());
	}
    
    // TODO: move to PythonState?
    private PythonObject getHelper(String name) {
        return pythonState.builtins.get(name);
    }
    
    private PythonObject helper(String name, Object... args) throws PythonException {
        PythonObject func = getHelper(name);
        return func.rawCall(args);
    }
    // END TODO
    
    public String toString() {
        try {
            PythonObject repr = pythonState.builtins.get("repr");
            Object result = repr.invoke(this);
            return this.getClass().getName() + "[" + result.toString() + "]";
        } catch (Exception e) {
            return super.toString();
        }
    }
    
    private void checkState(PythonState pyState) {
        if (pythonState != pyState) {
            throw new RuntimeException("invaild state (not match)");
        }
    }
    
    public Object invoke(Object... args) throws PythonException {
        return pythonState.jnupy_func_call(true, this, args);
    }
    
    public Object rawInvoke(Object... args) throws PythonException {
        return pythonState.jnupy_func_call(false, this, args);
    }
    
    public PythonObject call(Object... args) throws PythonException {
        Object result = pythonState.jnupy_func_call(true, this, args);
        if (result == null) {
            return null;
        }
        
        return PythonObject.fromObject(result);
    }
    
    public PythonObject rawCall(Object... args) throws PythonException {
        return PythonObject.fromObject(pythonState.jnupy_func_call(false, this, args));
    }
    
    // TODO: getitem, getattr, etc impl in here? (by builtin)

    public PythonObject attr(String name) throws PythonException {
        return getattr(name);
    }

    public PythonObject attr(String name, Object value) throws PythonException {
        return setattr(name, value);
    }

    public PythonObject getattr(String name) throws PythonException {
        return helper("getattr", this, name);
    }

    public PythonObject getattr(String name, Object defvalue) throws PythonException {
        return helper("getattr", this, name, defvalue);
    }

    public PythonObject setattr(String name, Object value) throws PythonException {
        return helper("setattr", this, name, value);
    }
    
    public PythonObject hasattr(String name) throws PythonException {
        return helper("hasattr", this, name);
    }
    
    public PythonObject delattr(String name) throws PythonException {
        return helper("delattr", this, name);
    }
    
    public Object unbox() throws PythonException {
        return pythonState.helpers.get("unbox").invoke(this);
    }
    
    // getitem or setitem require helper function... (not builtin function.)
}
