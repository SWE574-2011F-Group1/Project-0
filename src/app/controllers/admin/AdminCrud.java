package controllers.admin;

import play.*;
import play.mvc.*;

import java.util.*;

import models.*;

import controllers.CRUD;
import controllers.Secure;
import controllers.Check;

@With(Secure.class)
@Check("admin")
public class AdminCrud extends CRUD {
}

