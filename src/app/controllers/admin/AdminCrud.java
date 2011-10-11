package controllers.admin;

import play.*;
import play.mvc.*;

import java.util.*;

import models.*;

import controllers.CRUD;
import controllers.Secure;

@With(Secure.class)
public class AdminCrud extends CRUD {
}

