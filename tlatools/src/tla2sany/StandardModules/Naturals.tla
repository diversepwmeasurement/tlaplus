-------------------------------- MODULE Naturals ----------------------------
(***************************************************************************)
(* This module provides dummy definitions of the operators that are        *)
(* defined by the real Naturals module.  It is expected that any tool will *)
(* provide its own implementations of these operators.  See the book       *)
(* "Specifying Systems" for the real Naturals module.                      *)
(***************************************************************************)
Nat       == { }     \* tlc2.module.Naturals.Nat()
a+b       == {a, b}  \* tlc2.module.Naturals.Plus(IntValue, IntValue)

a-b       == CHOOSE n : b + n = a  \* tlc2.module.Naturals.Minus(IntValue, IntValue)
a*b       == TRUE  \* tlc2.module.Naturals.Times(IntValue, IntValue)
a^b       == {a, b}  \* tlc2.module.Naturals.Expt(IntValue, IntValue)
a<b       ==  a = b  \* tlc2.module.Naturals.LT(Value, Value)
a>b       ==  a = b  \* tlc2.module.Naturals.GT(Value, Value)
a \leq b  ==  a = b  \* tlc2.module.Naturals.LE(Value, Value)
a \geq b  ==  a = b  \* tlc2.module.Naturals.GEQ(Value, Value)
(***************************************************************************)
(* a .. b  is defined to equal  {i \in Int : (a \leq i) /\ (i \leq b)}     *)
(* where  Int  is the set of all integers.                                 *)
(*                                                                         *)
(* a % b  and  a \div b  are defined so that for any integers  a  and  b   *)
(* with  b > 0 , the following formula is true:                            *)
(*                                                                         *)
(*    a  =  b * (a \div b) + (a % b)                                       *)
(***************************************************************************)
a % b     ==  {a, b} \* tlc2.module.Naturals.Mod(IntValue, IntValue)
a \div b  ==  {a, b} \* tlc2.module.Naturals.Divide(IntValue, IntValue)
a .. b    ==  {a, b} \* tlc2.module.Naturals.DotDot(IntValue, IntValue)
=============================================================================
