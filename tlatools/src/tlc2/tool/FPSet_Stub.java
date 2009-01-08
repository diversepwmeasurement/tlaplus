// Stub class generated by rmic, do not edit.
// Contents subject to change without notice.

package tlc2.tool;

public final class FPSet_Stub
    extends java.rmi.server.RemoteStub
    implements tlc2.tool.FPSetRMI, java.rmi.Remote
{
    private static final java.rmi.server.Operation[] operations = {
	new java.rmi.server.Operation("void beginChkpt(java.lang.String)"),
	new java.rmi.server.Operation("void commitChkpt(java.lang.String)"),
	new java.rmi.server.Operation("boolean contains(long)"),
	new java.rmi.server.Operation("tlc2.util.BitVector containsBlock(tlc2.util.LongVec)"),
	new java.rmi.server.Operation("void exit(boolean)"),
	new java.rmi.server.Operation("boolean put(long)"),
	new java.rmi.server.Operation("tlc2.util.BitVector putBlock(tlc2.util.LongVec)"),
	new java.rmi.server.Operation("void recover(java.lang.String)"),
	new java.rmi.server.Operation("long size()")
    };
    
    private static final long interfaceHash = 1285648677324221003L;
    
    private static final long serialVersionUID = 2;
    
    private static boolean useNewInvoke;
    private static java.lang.reflect.Method $method_beginChkpt_0;
    private static java.lang.reflect.Method $method_commitChkpt_1;
    private static java.lang.reflect.Method $method_contains_2;
    private static java.lang.reflect.Method $method_containsBlock_3;
    private static java.lang.reflect.Method $method_exit_4;
    private static java.lang.reflect.Method $method_put_5;
    private static java.lang.reflect.Method $method_putBlock_6;
    private static java.lang.reflect.Method $method_recover_7;
    private static java.lang.reflect.Method $method_size_8;
    
    static {
	try {
	    java.rmi.server.RemoteRef.class.getMethod("invoke",
		new java.lang.Class[] {
		    java.rmi.Remote.class,
		    java.lang.reflect.Method.class,
		    java.lang.Object[].class,
		    long.class
		});
	    useNewInvoke = true;
	    $method_beginChkpt_0 = tlc2.tool.FPSetRMI.class.getMethod("beginChkpt", new java.lang.Class[] {java.lang.String.class});
	    $method_commitChkpt_1 = tlc2.tool.FPSetRMI.class.getMethod("commitChkpt", new java.lang.Class[] {java.lang.String.class});
	    $method_contains_2 = tlc2.tool.FPSetRMI.class.getMethod("contains", new java.lang.Class[] {long.class});
	    $method_containsBlock_3 = tlc2.tool.FPSetRMI.class.getMethod("containsBlock", new java.lang.Class[] {tlc2.util.LongVec.class});
	    $method_exit_4 = tlc2.tool.FPSetRMI.class.getMethod("exit", new java.lang.Class[] {boolean.class});
	    $method_put_5 = tlc2.tool.FPSetRMI.class.getMethod("put", new java.lang.Class[] {long.class});
	    $method_putBlock_6 = tlc2.tool.FPSetRMI.class.getMethod("putBlock", new java.lang.Class[] {tlc2.util.LongVec.class});
	    $method_recover_7 = tlc2.tool.FPSetRMI.class.getMethod("recover", new java.lang.Class[] {java.lang.String.class});
	    $method_size_8 = tlc2.tool.FPSetRMI.class.getMethod("size", new java.lang.Class[] {});
	} catch (java.lang.NoSuchMethodException e) {
	    useNewInvoke = false;
	}
    }
    
    // constructors
    public FPSet_Stub() {
	super();
    }
    public FPSet_Stub(java.rmi.server.RemoteRef ref) {
	super(ref);
    }
    
    // methods from remote interfaces
    
    // implementation of beginChkpt(String)
    public void beginChkpt(java.lang.String $param_String_1)
	throws java.io.IOException
    {
	try {
	    if (useNewInvoke) {
		ref.invoke(this, $method_beginChkpt_0, new java.lang.Object[] {$param_String_1}, 5803381104921595345L);
	    } else {
		java.rmi.server.RemoteCall call = ref.newCall((java.rmi.server.RemoteObject) this, operations, 0, interfaceHash);
		try {
		    java.io.ObjectOutput out = call.getOutputStream();
		    out.writeObject($param_String_1);
		} catch (java.io.IOException e) {
		    throw new java.rmi.MarshalException("error marshalling arguments", e);
		}
		ref.invoke(call);
		ref.done(call);
	    }
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (java.io.IOException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
    
    // implementation of commitChkpt(String)
    public void commitChkpt(java.lang.String $param_String_1)
	throws java.io.IOException
    {
	try {
	    if (useNewInvoke) {
		ref.invoke(this, $method_commitChkpt_1, new java.lang.Object[] {$param_String_1}, 4680511091156857157L);
	    } else {
		java.rmi.server.RemoteCall call = ref.newCall((java.rmi.server.RemoteObject) this, operations, 1, interfaceHash);
		try {
		    java.io.ObjectOutput out = call.getOutputStream();
		    out.writeObject($param_String_1);
		} catch (java.io.IOException e) {
		    throw new java.rmi.MarshalException("error marshalling arguments", e);
		}
		ref.invoke(call);
		ref.done(call);
	    }
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (java.io.IOException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
    
    // implementation of contains(long)
    public boolean contains(long $param_long_1)
	throws java.io.IOException
    {
	try {
	    if (useNewInvoke) {
		Object $result = ref.invoke(this, $method_contains_2, new java.lang.Object[] {new java.lang.Long($param_long_1)}, 6451628724969884264L);
		return ((java.lang.Boolean) $result).booleanValue();
	    } else {
		java.rmi.server.RemoteCall call = ref.newCall((java.rmi.server.RemoteObject) this, operations, 2, interfaceHash);
		try {
		    java.io.ObjectOutput out = call.getOutputStream();
		    out.writeLong($param_long_1);
		} catch (java.io.IOException e) {
		    throw new java.rmi.MarshalException("error marshalling arguments", e);
		}
		ref.invoke(call);
		boolean $result;
		try {
		    java.io.ObjectInput in = call.getInputStream();
		    $result = in.readBoolean();
		} catch (java.io.IOException e) {
		    throw new java.rmi.UnmarshalException("error unmarshalling return", e);
		} finally {
		    ref.done(call);
		}
		return $result;
	    }
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (java.io.IOException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
    
    // implementation of containsBlock(LongVec)
    public tlc2.util.BitVector containsBlock(tlc2.util.LongVec $param_LongVec_1)
	throws java.io.IOException
    {
	try {
	    if (useNewInvoke) {
		Object $result = ref.invoke(this, $method_containsBlock_3, new java.lang.Object[] {$param_LongVec_1}, -5675636475111513139L);
		return ((tlc2.util.BitVector) $result);
	    } else {
		java.rmi.server.RemoteCall call = ref.newCall((java.rmi.server.RemoteObject) this, operations, 3, interfaceHash);
		try {
		    java.io.ObjectOutput out = call.getOutputStream();
		    out.writeObject($param_LongVec_1);
		} catch (java.io.IOException e) {
		    throw new java.rmi.MarshalException("error marshalling arguments", e);
		}
		ref.invoke(call);
		tlc2.util.BitVector $result;
		try {
		    java.io.ObjectInput in = call.getInputStream();
		    $result = (tlc2.util.BitVector) in.readObject();
		} catch (java.io.IOException e) {
		    throw new java.rmi.UnmarshalException("error unmarshalling return", e);
		} catch (java.lang.ClassNotFoundException e) {
		    throw new java.rmi.UnmarshalException("error unmarshalling return", e);
		} finally {
		    ref.done(call);
		}
		return $result;
	    }
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (java.io.IOException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
    
    // implementation of exit(boolean)
    public void exit(boolean $param_boolean_1)
	throws java.io.IOException
    {
	try {
	    if (useNewInvoke) {
		ref.invoke(this, $method_exit_4, new java.lang.Object[] {new java.lang.Boolean($param_boolean_1)}, 4397593303839472716L);
	    } else {
		java.rmi.server.RemoteCall call = ref.newCall((java.rmi.server.RemoteObject) this, operations, 4, interfaceHash);
		try {
		    java.io.ObjectOutput out = call.getOutputStream();
		    out.writeBoolean($param_boolean_1);
		} catch (java.io.IOException e) {
		    throw new java.rmi.MarshalException("error marshalling arguments", e);
		}
		ref.invoke(call);
		ref.done(call);
	    }
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (java.io.IOException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
    
    // implementation of put(long)
    public boolean put(long $param_long_1)
	throws java.io.IOException
    {
	try {
	    if (useNewInvoke) {
		Object $result = ref.invoke(this, $method_put_5, new java.lang.Object[] {new java.lang.Long($param_long_1)}, 602730934478900184L);
		return ((java.lang.Boolean) $result).booleanValue();
	    } else {
		java.rmi.server.RemoteCall call = ref.newCall((java.rmi.server.RemoteObject) this, operations, 5, interfaceHash);
		try {
		    java.io.ObjectOutput out = call.getOutputStream();
		    out.writeLong($param_long_1);
		} catch (java.io.IOException e) {
		    throw new java.rmi.MarshalException("error marshalling arguments", e);
		}
		ref.invoke(call);
		boolean $result;
		try {
		    java.io.ObjectInput in = call.getInputStream();
		    $result = in.readBoolean();
		} catch (java.io.IOException e) {
		    throw new java.rmi.UnmarshalException("error unmarshalling return", e);
		} finally {
		    ref.done(call);
		}
		return $result;
	    }
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (java.io.IOException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
    
    // implementation of putBlock(LongVec)
    public tlc2.util.BitVector putBlock(tlc2.util.LongVec $param_LongVec_1)
	throws java.io.IOException
    {
	try {
	    if (useNewInvoke) {
		Object $result = ref.invoke(this, $method_putBlock_6, new java.lang.Object[] {$param_LongVec_1}, -3755190722096070151L);
		return ((tlc2.util.BitVector) $result);
	    } else {
		java.rmi.server.RemoteCall call = ref.newCall((java.rmi.server.RemoteObject) this, operations, 6, interfaceHash);
		try {
		    java.io.ObjectOutput out = call.getOutputStream();
		    out.writeObject($param_LongVec_1);
		} catch (java.io.IOException e) {
		    throw new java.rmi.MarshalException("error marshalling arguments", e);
		}
		ref.invoke(call);
		tlc2.util.BitVector $result;
		try {
		    java.io.ObjectInput in = call.getInputStream();
		    $result = (tlc2.util.BitVector) in.readObject();
		} catch (java.io.IOException e) {
		    throw new java.rmi.UnmarshalException("error unmarshalling return", e);
		} catch (java.lang.ClassNotFoundException e) {
		    throw new java.rmi.UnmarshalException("error unmarshalling return", e);
		} finally {
		    ref.done(call);
		}
		return $result;
	    }
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (java.io.IOException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
    
    // implementation of recover(String)
    public void recover(java.lang.String $param_String_1)
	throws java.io.IOException
    {
	try {
	    if (useNewInvoke) {
		ref.invoke(this, $method_recover_7, new java.lang.Object[] {$param_String_1}, -5352331576049440621L);
	    } else {
		java.rmi.server.RemoteCall call = ref.newCall((java.rmi.server.RemoteObject) this, operations, 7, interfaceHash);
		try {
		    java.io.ObjectOutput out = call.getOutputStream();
		    out.writeObject($param_String_1);
		} catch (java.io.IOException e) {
		    throw new java.rmi.MarshalException("error marshalling arguments", e);
		}
		ref.invoke(call);
		ref.done(call);
	    }
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (java.io.IOException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
    
    // implementation of size()
    public long size()
	throws java.io.IOException
    {
	try {
	    if (useNewInvoke) {
		Object $result = ref.invoke(this, $method_size_8, null, -4918956806860670663L);
		return ((java.lang.Long) $result).longValue();
	    } else {
		java.rmi.server.RemoteCall call = ref.newCall((java.rmi.server.RemoteObject) this, operations, 8, interfaceHash);
		ref.invoke(call);
		long $result;
		try {
		    java.io.ObjectInput in = call.getInputStream();
		    $result = in.readLong();
		} catch (java.io.IOException e) {
		    throw new java.rmi.UnmarshalException("error unmarshalling return", e);
		} finally {
		    ref.done(call);
		}
		return $result;
	    }
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (java.io.IOException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
}
