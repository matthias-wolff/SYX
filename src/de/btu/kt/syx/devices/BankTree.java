package de.btu.kt.syx.devices;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import de.btu.kt.syx.SYX;

/**
 * A tree whose leaves are {@linkplain IBankObject bank objects}, e.g.,
 * {@linkplain IPatch patches} or {@linkplain IMulti multis}. There are three
 * types of nodes:
 * <ul style=" margin-bottom:0">
 *   <li>{@linkplain BankObject Bank object nodes} represent or {@linkplain
 *     BankObject#getObject() contain} a single {@linkplain T bank object}. They
 *     have a {@linkplain BankObject#getUID() unique identifier} in their
 *     {@linkplain Bank bank node}.</li>
 *   <li>{@linkplain Bank Bank nodes} contain a list of {@linkplain BankObject
 *     bank object nodes}. They have a {@linkplain #getRoot() globally}
 *     {@linkplain Bank#getUID() unique identifier} in the bank tree.</li>
 *   <li>{@linkplain BankTree Tree nodes} contain a list of {@linkplain BankTree
 *     tree nodes} and/or a list of {@linkplain Bank bank nodes}. They have a
 *     {@linkplain #getUID() unique identifier} in their {@linkplain
 *     #getParent() parent node}.</li>
 * </ul>
 *
 * <p><b>Example 1: Patch Bank with {@linkplain IPatch Patch Data
 * Model}</b><pre>
 *   VZ1Patch vzPatch1 = &hellip;;
 *       &vellip;
 *   VZ1Patch vzPatch192 = &hellip;;
 *
 *   BankTree&lt;VZ1Patch&gt; patchBank = new {@link #BankTree(String, String) BankTree}&lt;VZ1Patch&gt;("Patch","VZ Factory Patches");
 *   patchBank.{@link #addBank(String, String) addBank}("I","INTERNAL");
 *   patchBank.{@link #addBank(String, String) addBank}("C(1)","CARD 1");
 *   patchBank.{@link #addBank(String, String) addBank}("C(2)","CARD 2");
 *
 *   patchBank.{@link #addBankObject(String, String, T) addBankObject}("I","A-1",vzPatch1);
 *       &vellip;
 *   patchBank.{@link #addBankObject(String, String, T) addBankObject}("C(2)","H-8",vzPatch192);
 *
 *   VZ1Patch patch = patchBank.{@link #getBankObject(String, String) getBankObject}("I","B-7");
 *       &vellip;</pre></p>
 *
 * <p><b>Example 2: Patch Bank without Patch Data Model:</b><pre>
 *   BankTree&lt;{@link BankTree.IBankObject}&gt; patchBank = new {@link  #BankTree(String, String) BankTree}&lt;{@link BankTree.IBankObject}&gt;("Patch","VZ Factory Patches");
 *   patchBank.{@link #addBank(String, String) addBank}("I","INTERNAL");
 *   patchBank.{@link #addBank(String, String) addBank}("C(1)","CARD 1");
 *   patchBank.{@link #addBank(String, String) addBank}("C(2)","CARD 2");
 *
 *   patchBank.{@link #addBankObject(String, String, String) addBankObject}("I","A-1","VZ EP");
 *       &vellip;
 *   patchBank.{@link #addBankObject(String, String, String) addBankObject}("C(2)","H-8","INIT VOICE");
 *
 *   {@link String} patchName = patchBank.{@link #getBankObjectName(String, String) getBankObjectName}("I","B-7");
 *       &vellip;</pre></p>
 *
 * @param <T>
 *          The bank object type, e.g., a {@linkplain IPatch patch} or a
 *          {@linkplain IMulti multi} type
 * 
 * @author Matthias Wolff
 */
public class BankTree<T extends BankTree.IBankObject> implements Serializable
{

  // -- Constants -------------------------------------------------------------

  private static final long serialVersionUID = 1L;

  /**
   * Unique identifier of the root node.
   */
  public static transient final String ROOT_UID = "_root_";

  /**
   * Error message format string: UID not found.
   */
  protected static transient final String E_UID_NOTFOUND
    = "%s UID '%s' not found";

  /**
   * Error message format string: duplicate UID.
   */
  protected static transient final String E_UID_DUPLICATE
    = "%s UID '%s' is not unique in %s '%s'";

  /**
   * Error message format string: duplicate global bank UID.
   */
  protected static transient final String E_UID_GDUPLICATE
    = "Bank UID '%s' is not unique in bank tree";

  // -- Nested Classes --------------------------------------------------------

  /**
   * Interface to bank objects, e.g., {@linkplain IPatch patches} or {@linkplain
   * IMulti multis}, stored in a bank tree.
   */
  public interface IBankObject
  {

    /**
     * Returns the name of the data object.
     */
    public String getName();

  }

  /**
   * Represents or {@linkplain #getObject() contains} a single {@linkplain T
   * bank object}, e.g., a {@linkplain IPatch patch} or a {@linkplain IMulti
   * multi}, in a {@linkplain Bank bank}.
   * 
   * @author Matthias Wolff
   */
  public class BankObject implements Serializable
  {

    private static final long serialVersionUID = 1L;

    /**
     * The parent node.
     */
    private Bank parent;

    /**
     * The unique identifier of this node in its {@linkplain #parent parent
     * node}.
     */
    private String UID;

    /**
     * The name of this node.
     */
    private String name;

    /**
     * The {@linkplain T data object} stored in this node, can be {@code null}
     */
    private T object;

    /**
     * Internal constructor; creates a new bank object node <em>without</em>
     * a {@linkplain T bank object}.
     * 
     * @param parent
     *          The parent bank node
     * @param objectUID
     *          The unique identifier in {@code parent}
     * @param objectName
     *          The name of the bank object
     * @throws IllegalArgumentException if
     *          <ul style="margin-top:0;">
     *            <li>{@code parent} is {@code null}, or</li>
     *            <li>{@code objectUID} is {@code null} or empty, or</li>
     *            <li>{@code objectName} is {@code null} or empty</li>
     *          </ul>
     */
    private BankObject(Bank parent, String objectUID, String objectName)
    throws IllegalArgumentException
    {
      if (parent==null)
        throw SYX.IllArgExc(SYX.E_ARG_NULL,"parent");
      if (objectUID==null || objectUID.isEmpty())
        throw SYX.IllArgExc(SYX.E_ARG_NULLEMPTY,"objectUID");
      if (objectName==null || objectName.isEmpty())
        throw SYX.IllArgExc(SYX.E_ARG_NULLEMPTY,"objectName");

      this.parent = parent;
      this.UID    = objectUID;
      this.name   = objectName;
      this.object   = null;
    }

    /**
     * Internal constructor; creates a new bank object node <em>with</em> a bank
     * object.
     * 
     * @param parent
     *          The parent bank node
     * @param objectUID
     *          The unique identifier in {@code parent}
     * @param object
     *          The {@linkplain T bank object}
     * @throws IllegalArgumentException if
     *          <ul style="margin-top:0;">
     *            <li>{@code parent} is {@code null}, or</li>
     *            <li>{@code objectUID} is {@code null} or empty, or</li>
     *            <li>{@code object} is {@code null}</li>
     *          </ul>
     */
    private BankObject(Bank parent, String objectUID, T object)
    throws IllegalArgumentException
    {
      if (parent==null)
        throw SYX.IllArgExc(SYX.E_ARG_NULL,"parent");
      if (objectUID==null || objectUID.isEmpty())
        throw SYX.IllArgExc(SYX.E_ARG_NULLEMPTY,"objectUID");
      if (object==null)
        throw SYX.IllArgExc(SYX.E_ARG_NULL,"object");

      this.parent = parent;
      this.UID    = objectUID;
      this.name   = null;
      this.object = object;
    }

    /**
     * Returns the root node of the bank tree containing this node.
     */
    public BankTree<T> getRoot()
    {
      return this.parent.getRoot();
    }

    /**
     * Returns the {@link #parent parent} of this node.
     */
    public Bank getParent()
    {
      return this.parent;
    }

    /**
     * Returns the {@linkplain #UID unique identifier} of this node in its
     * {@linkplain #getParent() parent node}.
     */
    public String getUID()
    {
      return this.UID;
    }

    /**
     * Returns the name of this node. If a bank object is stored in this node,
     * the method returns {@link #object}{@code .}{@link
     * BankTree.IBankObject#getName() getName}{@code ()}, otherwise it returns
     * {@link #name}.
     */
    public String getName()
    {
      if (this.object!=null)
        return this.object.getName();
      return this.name;
    }

    /**
     * Returns the {@linkplain T bank object} stored in this node.
     * 
     * @return {@link #object}, may be {@code null}
     */
    public T getObject()
    {
      return this.object;
    }
    /**
     * Pretty-prints this node into a string.
     * 
     * @return The string
     */
    public String prettyPrint()
    {
      return int_prettyPrintTable(objectType,int_prettyPrint(""));
    }

   /**
    * Worker for {@link #prettyPrint()}.
    * 
    * @param linePrefix
    *          Prefix string containing the super-tree structure
    * @return A list of table rows, each containing three columns: tree
    *        structure, element UID, and element name
    */
    private List<String[]> int_prettyPrint(String linePrefix)
    {
      // Print this node
      String pfx  = linePrefix+objectType+(this.object!=null ? "*" : "");
      String UID  = this.UID  !=null ? this.UID  : "";
      String name = getName() !=null ? getName() : "";

      // Make print columns
      List<String[]> cols = new ArrayList<String[]>();
      cols.add(new String[]{pfx,UID,name});

      // Return printed columns
      return cols;
    }

  }

  /**
   * Contains a list of {@linkplain BankObject bank object nodes} in a
   * {@linkplain BankTree bank tree}.
   * 
   * @author Matthias Wolff
   */
  public class Bank implements Serializable
  {

    private static final long serialVersionUID = 1L;

    /**
     * The parent node.
     */
    private BankTree<T> parent;

    /**
     * The the globally unique identifier of this node in the {@linkplain
     * #getRoot() bank tree}.
     */
    private String UID;

    /**
     * The name of this node.
     */
    private String name;

    /**
     * The list of child nodes.
     */
    private LinkedHashMap<String,BankObject> children;

    /**
     * Internal constructor; creates a new bank node
     * 
     * @param parent
     *          The parent tree node
     * @param bankUID
     *          The globally unique identifier in the {@linkplain #getRoot()
     *          bank tree}
     * @param bankName
     *          The bank name, may be {@code null} or empty (discouraged)
     * @throws IllegalArgumentException if
     *          <ul style="margin-top:0;">
     *            <li>{@code parent} is {@code null}, or</li>
     *            <li>{@code bankUID} is {@code null} or empty</li>
     *          </ul>
     */
    private Bank(BankTree<T> parent, String bankUID, String bankName)
    throws IllegalArgumentException
    {
      if (parent==null)
        throw SYX.IllArgExc(SYX.E_ARG_NULL,"parent");
      if (bankUID==null || bankUID.isEmpty())
        throw SYX.IllArgExc(SYX.E_ARG_NULLEMPTY,"bankUID");

      this.parent   = parent;
      this.UID      = bankUID;
      this.name     = bankName;
      this.children = new LinkedHashMap<String,BankObject>();
      reset();
    }

    /**
     * Clears the {@linkplain #children list of child nodes}.
     */
    public void reset()
    {
      this.children.clear();
    }

    /**
     * Returns the root node of the bank tree containing this node.
     */
    public BankTree<T> getRoot()
    {
      return parent.getRoot();
    }

    /**
     * Returns the {@link #parent parent} of this node.
     */
    public BankTree<T> getParent()
    {
      return this.parent;
    }

    /**
     * Returns the globally {@linkplain #UID unique identifier} of this node in
     * the {@linkplain #getRoot() bank tree}.
     */
    public String getUID()
    {
      return this.UID;
    }

    /**
     * Returns the {@link #name name} of this node.
     * 
     * @return The name, may be {@code null}
     */
    public String getName()
    {
      return this.name;
    }

    /**
     * Adds a bank object node <em>without</em> a {@linkplain T bank object} to
     * this bank.
     * 
     * @param objectUID
     *          The unique object identifier in this bank
     * @param objectName
     *          The object name
     * @return The newly created {@linkplain BankObject bank object node}
     * @throws IllegalArgumentException if
     *          <ul style="margin-top:0;">
     *            <li>{@code objectUID} is {@code null} or empty, or not unique,
     *              or</li>
     *            <li>{@code objectName} is {@code null} or empty</li>
     *          </ul>
     */
    public BankObject addBankObject(String objectUID, String objectName)
    throws IllegalArgumentException
    {
      if (this.children.containsKey(objectUID))
        throw SYX.IllArgExc(E_UID_DUPLICATE,"Object",objectUID,"bank",this.UID);

      BankObject child = new BankObject(this,objectUID,objectName);
      this.children.put(objectUID,child);
      return child;
    }

    /**
     * Adds a bank object node <em>with</em> a bank object to this bank.
     * 
     * @param objectUID
     *          The unique object identifier in this bank
     * @param object
     *          The {@linkplain T bank object}
     * @return The newly created {@linkplain BankObject bank object node}
     * @throws IllegalArgumentException if
     *          <ul style="margin-top:0;">
     *            <li>{@code objectUID} is {@code null} or empty, or not unique,
     *              or</li>
     *            <li>{@code object} is {@code null}</li>
     *          </ul>
     */
    public BankObject addBankObject(String objectUID, T object)
    throws IllegalArgumentException
    {
      if (this.children.containsKey(objectUID))
        throw SYX.IllArgExc(E_UID_DUPLICATE,"Object",objectUID,"bank",this.UID);

      BankObject child = new BankObject(this,objectUID,object);
      this.children.put(objectUID,child);
      return child;
    }

    /**
     * Returns the unique identifiers of the bank objects in this bank.
     * 
     * @return A list of {@linkplain BankObject bank object} UIDs, may be empty
     */
    public List<String> getBankObjectUIDs()
    {
      ArrayList<String> UIDs = new ArrayList<String>();
      UIDs.addAll(this.children.keySet());
      return UIDs;
    }

    /**
     * Returns the bank objects in this bank.
     * 
     * @return A list of {@linkplain BankObject bank object nodes}, may be empty
     */
    public List<BankObject> getBankObjects()
    {
      ArrayList<BankObject> objects = new ArrayList<BankObject>();
      objects.addAll(this.children.values());
      return objects;
    }

    /**
     * Returns a bank object in this bank.
     * 
     * @param objectUID
     *          The unique object identifier in this bank
     * @return The {@linkplain T bank object}
     * @throws IllegalArgumentException
     *          if {@code objectUID} is {@code null} or empty, or if no such
     *          object exists
     */
    public T getBankObject(String objectUID)
    throws IllegalArgumentException
    {
      if (objectUID==null || objectUID.isEmpty())
        throw SYX.IllArgExc(SYX.E_ARG_NULLEMPTY,"objectUID");

      BankObject child = this.children.get(objectUID);
      if (child==null)
        throw SYX.IllArgExc(E_UID_NOTFOUND,"Object",objectUID);
      return child.object;
    }

    /**
     * Returns the name of a bank object in this bank.
     * 
     * @param objectUID
     *          The unique object identifier in this bank
     * @return The name
     * @throws IllegalArgumentException
     *          if {@code objectUID} is {@code null} or empty, or if no such
     *          object exists
     */
    public String getBankObjectName(String objectUID)
    {
      if (objectUID==null || objectUID.isEmpty())
        throw SYX.IllArgExc(SYX.E_ARG_NULLEMPTY,"objectUID");
      BankObject child = this.children.get(objectUID);
      if (child==null)
        throw SYX.IllArgExc(E_UID_NOTFOUND,"Object",objectUID);
      return child.object.getName();
    }

    /**
     * Pretty-prints this node into a string.
     * 
     * @return The string
     */
    public String prettyPrint()
    {
      return int_prettyPrintTable(objectType+" Bank",int_prettyPrint(""));
    }

    /**
     * Worker for {@link #prettyPrint()}.
     * 
     * @param linePrefix
     *          Prefix string containing the super-tree structure
     * @return A list of table rows, each containing three columns: tree
     *        structure, element UID, and element name
     */
    private List<String[]> int_prettyPrint(String linePrefix)
    {
      // Print this node
      String pfx  = linePrefix+objectType+" Bank";
      String UID  = this.UID !=null ? this.UID  : "";
      String name = this.name!=null ? this.name : "";

      // Make print columns
      List<String[]> cols = new ArrayList<String[]>();
      cols.add(new String[]{pfx,UID,name});

      // Print children
      List<BankObject> bankObjects = getBankObjects();
      for (int i=0; i<bankObjects.size(); i++)
      {
        String lpfx = linePrefix.replace("'-"," ")
                    + (i<bankObjects.size()-1 ? "|- " : "'- ");
        cols.addAll(bankObjects.get(i).int_prettyPrint(lpfx));
      }

      // Return printed columns
      return cols;
    }

  }

  // -- Attributes ------------------------------------------------------------

  /**
   * The parent node.
   */
  private BankTree<T> parent;

  /**
   * The unique identifier of this node in its {@linkplain #parent parent node}.
   */
  private String UID;

  /**
   * A human-readable type name of {@linkplain IBankObject bank objects} stored
   * in this bank tree, e.g., "Patch" or "Multi"
   */
  private String objectType;

  /**
   * The name of this node.
   */
  private String name;

  /**
   * The list of {@linkplain BankTree tree} children.
   */
  private LinkedHashMap<String,BankTree<T>> treeChilderen;

  /**
   * The list of {@linkplain Bank bank} children.
   */
  private LinkedHashMap<String,Bank> bankChildren;

  // -- Constructors and Initialization ---------------------------------------

  /**
   * Internal constructor; creates a new tree node.
   * 
   * @param parent
   *          The parent tree node; {@code null} for the root node
   * @param UID
   *          The unique identifier in {@code parent}, must not be {@code null}
   *          or empty; {@link #ROOT_UID} for the root node
   * @param objectType
   *          Human-readable type name of {@linkplain IBankObject bank objects}
   *          stored in this bank tree, e.g., "Patch" or "Multi"
   * @param name
   *          The name, can be {@code null} (discouraged)
   * @throws IllegalArgumentException
   *          if {@code UID} is {@code null} or empty
   */
  private BankTree
  (
    BankTree<T> parent,
    String      UID,
    String      objectType,
    String      name
  )
  throws IllegalArgumentException
  {
    if (UID==null || UID.isEmpty())
      throw SYX.IllArgExc(SYX.E_ARG_NULLEMPTY,"UID");

    this.parent        = parent;
    this.UID           = UID;
    this.objectType    = objectType;
    this.name          = name;
    this.treeChilderen = new LinkedHashMap<String,BankTree<T>>();
    this.bankChildren  = new LinkedHashMap<String,Bank>();
    reset();
  }

  /**
   * Creates a bank tree.
   * 
   * @param objectType
   *          Human-readable type name of {@linkplain IBankObject bank objects}
   *          stored in this bank tree, e.g., "Patch" or "Multi"
   * @param name
   *          The name, can be {@code null} (discouraged)
   */
  public BankTree(String objectType, String name)
  {
    this(null,ROOT_UID,objectType,name);
  }

  /**
   * Clears the {@linkplain #treeChilderen list of tree children} and the
   * {@linkplain #bankChildren list of bank children}.
   */
  public void reset()
  {
    this.treeChilderen.clear();
    this.bankChildren.clear();
  }

  // -- Getters ---------------------------------------------------------------

  /**
   * Returns the root node of the bank tree containing this node.
   * 
   * @return The root node; {@code this} if this node is the root
   */
  public BankTree<T> getRoot()
  {
    if (this.parent==null)
      return this;
    return this.parent.getRoot();
  }

  /**
   * Returns the {@link #parent parent} of this node.
   * 
   * @return The parent node; {@code null} if this node is the root
   */
  public BankTree<T> getParent()
  {
    return this.parent;
  }

  /**
   * Returns the {@linkplain #UID unique identifier} of this node in its
   * {@linkplain #getParent() parent node}.
   */
  public String getUID()
  {
    return this.UID;
  }

  /**
   * Returns the {@link #name name} of this node.
   * 
   * @return The name, may be {@code null}
   */
  public String getName()
  {
    return this.name;
  }

  // -- Add and Get Trees -----------------------------------------------------

  /**
   * Adds a sub-tree to this tree.
   * 
   * @param UID
   *          The unique identifier of the sub-tree in this tree
   * @param name
   *          The sub-tree name, can be {@code null} or empty (discouraged) 
   * @return The newly created {@linkplain BankTree tree node}
   * @throws IllegalArgumentException
   *          if {@code UID} is {@code null} or empty, or not unique,
   */
  public BankTree<T> addTree(String UID, String name)
  throws IllegalArgumentException
  {
    if (this.treeChilderen.containsKey(UID))
      throw SYX.IllArgExc(E_UID_DUPLICATE,"Tree",UID,"tree",this.UID);

    BankTree<T> treeNode = new BankTree<T>(this,UID,this.objectType,name);
    this.treeChilderen.put(UID,treeNode);
    return treeNode;
  }

  /**
   * Returns the unique identifiers of the sub-trees of this tree.
   * 
   * @return A list of {@linkplain BankTree tree} UIDs, may be empty
   */
  public List<String> getTreeUIDs()
  {
    ArrayList<String> UIDs = new ArrayList<String>();
    UIDs.addAll(this.treeChilderen.keySet());
    return UIDs;
  }

  /**
   * Returns the sub-trees of this tree.
   * 
   * @return A list of {@linkplain BankTree tree nodes}, may be empty
   */
  public List<BankTree<T>> getTrees()
  {
    ArrayList<BankTree<T>> trees = new ArrayList<BankTree<T>>();
    trees.addAll(this.treeChilderen.values());
    return trees;
  }

  /**
   * Returns a sub-tree of this tree.
   * 
   * @param UID
   *          The unique identifier of the sub-tree
   * @return The {@linkplain BankTree sub-tree node}
   * @throws IllegalArgumentException
   *          if {@code UID} is {@code null} or empty, or if no such sub-tree
   *          exists
   */
  public BankTree<T> getTree(String UID)
  throws IllegalArgumentException
  {
    if (UID==null || UID.isEmpty())
      throw SYX.IllArgExc(SYX.E_ARG_NULLEMPTY,"UID");

    BankTree<T> treeNode = treeChilderen.get(UID);
    if (treeNode==null)
      throw SYX.IllArgExc(E_UID_NOTFOUND,"Tree",UID);
    return treeNode;
  }

  // -- Add and Get Banks -----------------------------------------------------

  /**
   * Adds a bank to this tree.
   * 
   * @param bankUID
   *          The globally unique identifier in the {@linkplain #getRoot()
   *          bank tree}
   * @param bankName
   *          The bank name, can be {@code null} or empty (discouraged)
   * @return The newly created {@linkplain Bank bank node}
   * @throws IllegalArgumentException
   *          if {@code bankUID} is {@code null} or empty, or not globally
   *          unique,
   */
  public Bank addBank(String bankUID, String bankName)
  throws IllegalArgumentException
  {
    try
    {
      getRoot().getBank(bankUID,true);
      throw SYX.IllArgExc(E_UID_GDUPLICATE,bankUID);
    }
    catch (IllegalArgumentException e)
    { // Ok, no such bank exists already
    }

    Bank bank = new Bank(this,bankUID,bankName);
    this.bankChildren.put(bank.getUID(),bank);
    return bank;
  }

  /**
   * Returns the unique identifiers of the {@linkplain Bank banks} in this tree.
   * 
   * @param recursive
   *          if {@code true}, the method recursively lists the bank UIDs in
   *          this tree and its sub-trees (if any); otherwise the method only
   *          lists the bank UIDs in this tree
   * @return A list of {@linkplain Bank bank} UIDs, may be empty
   */
  public List<String> getBankUIDs(boolean recursive)
  {
    // List bank UIDs in this tree
    ArrayList<String> UIDs = new ArrayList<String>();
    UIDs.addAll(this.bankChildren.keySet());
    if (!recursive)
      return UIDs;

    // Recursively list bank UIDs in sub-trees
    for (BankTree<T> child : getTrees())
      UIDs.addAll(child.getBankUIDs(true));
    return UIDs;
  }

  /**
   * Returns the banks in this tree.
   * 
   * @param recursive
   *          if {@code true}, the method recursively lists the banks in this
   *          tree and its sub-trees (if any); otherwise the method only lists
   *          the banks in this tree
   * @return A list of {@linkplain Bank bank nodes}, may be empty
   */
  public List<Bank> getBanks(boolean recursive)
  {
    ArrayList<Bank> banks = new ArrayList<Bank>();
    banks.addAll(this.bankChildren.values());
    if (!recursive)
      return banks;

    // Recursively list banks in sub-trees
    for (BankTree<T> child : getTrees())
      banks.addAll(child.getBanks(true));
    return banks;
  }

  /**
   * Returns a bank in this tree.
   * 
   * @param bankUID
   *          The globally unique identifier in the {@linkplain #getRoot()
   *          bank tree}
   * @param recursive
   *          if {@code true}, the method recursively searches the bank in this
   *          tree and its sub-trees (if any); otherwise the method only
   *          searches the bank in this tree
   * @return The {@linkplain Bank bank node}
   * @throws IllegalArgumentException
   *          if {@code bankUID} is {@code null} or empty, or if no such bank
   *          exists
   */
  public Bank getBank(String bankUID, boolean recursive)
  throws IllegalArgumentException
  {
    if (bankUID==null || bankUID.isEmpty())
      throw SYX.IllArgExc(SYX.E_ARG_NULLEMPTY,"bankUID");

    Bank bank = this.bankChildren.get(bankUID);
    if (bank!=null || !recursive)
      return bank;

    // Recursively search in sub-trees
    for (BankTree<T> child : getTrees())
    {
      bank = child.getBank(bankUID,true);
      if (bank!=null)
        return bank;
    }

    // Bank not found
    throw SYX.IllArgExc(E_UID_NOTFOUND,"Bank",bankUID);
  }

  // -- Add and Get Bank Objects ----------------------------------------------

  /**
   * Adds a bank object node <em>without</em> a {@linkplain T bank object} to a
   * {@linkplain Bank bank} in this tree or in one of its sub-trees. Convenience
   * shortcut for {@link #getBank(String, boolean) getBank}{@code
   * (bankUID,true).}{@link Bank#addBankObject(String, String)
   * addBankObject}{@code (objectUID,objectName)}.
   * 
   * @param bankUID
   *          The globally unique bank identifier in the {@linkplain #getRoot()
   *          bank tree}
   * @param objectUID
   *          The unique object identifier in the bank identified by {@code
   *          bankUID}
   * @param objectName
   *          The object name
   * @return The newly created {@linkplain BankObject bank object node}
   * @throws IllegalArgumentException if
   *          <ul style="margin-top:0;">
   *            <li>{@code bankUID} is {@code null} or empty, or no such bank
   *              exists, or</li>
   *            <li>{@code objectUID} is {@code null} or empty, or not unique,
   *              or</li>
   *            <li>{@code objectName} is {@code null} or empty</li>
   *          </ul>
   */
  public BankObject addBankObject
  (
    String bankUID,
    String objectUID,
    String objectName
  )
  throws IllegalArgumentException
  {
    return getBank(bankUID,true).addBankObject(objectUID,objectName);
  }

  /**
   * Adds a bank object node <em>with</em> a bank object to a {@linkplain Bank
   * bank} in this tree or in one of its sub-trees. Convenience shortcut for
   * {@link #getBank(String, boolean) getBank}{@code (bankUID,true).}{@link
   * Bank#addBankObject(String, T) addBankObject}{@code
   * (objectUID,object)}.
   * 
   * @param bankUID
   *          The globally unique bank identifier in the {@linkplain #getRoot()
   *          bank tree}
   * @param objectUID
   *          The unique object identifier in the bank identified by {@code
   *          bankUID}
   * @param object
   *          The {@linkplain T bank object}
   * @return The newly created {@linkplain BankObject bank object node}
   * @throws IllegalArgumentException if
   *          <ul style="margin-top:0;">
   *            <li>{@code bankUID} is {@code null} or empty, or no such bank
   *              exists, or</li>
   *            <li>{@code objectUID} is {@code null} or empty, or not unique,
   *              or</li>
   *            <li>{@code object} is {@code null}</li>
   *          </ul>
   */
  public BankObject addBankObject(String bankUID, String objectUID, T object)
  throws IllegalArgumentException
  {
    return getBank(bankUID,true).addBankObject(objectUID,object);
  }

  /**
   * Returns a bank object in a {@linkplain Bank bank} in this tree or in one of
   * its sub-trees. Convenience shortcut for {@link #getBank(String, boolean)
   * getBank}{@code (bankUID,true).}{@link Bank#getBankObject(String)
   * getBankObject}{@code (objectUID)}.
   * 
   * @param bankUID
   *          The globally unique bank identifier in the {@linkplain #getRoot()
   *          bank tree}
   * @param objectUID
   *          The unique object identifier in the bank identified by {@code
   *          bankUID}
   * @return The {@linkplain T bank object}
   * @throws IllegalArgumentException if
   *          <ul style="margin-top:0;">
   *            <li>{@code bankUID} is {@code null} or empty, or no such bank
   *              exists, or</li>
   *            <li>{@code objectUID} is {@code null} or empty, or no such
   *              object exists in the bank identified by {@code bankUID}</li>
   *          </ul>
   */
  public T getBankObject(String bankUID, String objectUID)
  throws IllegalArgumentException
  {
    return getBank(objectUID,true).getBankObject(objectUID);
  }

  /**
   * Returns the name of a bank object in a {@linkplain Bank bank} in this tree
   * or in one of its sub-trees. Convenience shortcut for {@link
   * #getBank(String, boolean) getBank}{@code (bankUID,true).}{@link
   * Bank#getBankObjectName(String) getBankObjectName}{@code (objectUID)}.
   * 
   * @param bankUID
   *          The globally unique bank identifier in the {@linkplain #getRoot()
   *          bank tree}
   * @param objectUID
   *          The unique object identifier in the bank identified by {@code
   *          bankUID}
   * @return The name
   * @throws IllegalArgumentException if
   *          <ul style="margin-top:0;">
   *            <li>{@code bankUID} is {@code null} or empty, or no such bank
   *              exists, or</li>
   *            <li>{@code objectUID} is {@code null} or empty, or no such
   *              object exists in the bank identified by {@code bankUID}</li>
   *          </ul>
   */
  public String getBankObjectName(String bankUID, String objectUID)
  throws IllegalArgumentException
  {
    return getBank(objectUID,true).getBankObjectName(objectUID);
  }

  // -- Pretty-Printing -------------------------------------------------------

  /**
   * Pretty-prints this node into a string.
   * 
   * @return The string
   */
  public String prettyPrint()
  {
    return int_prettyPrintTable(this.objectType+" Bank",int_prettyPrint(""));
  }

  // -- Workers ---------------------------------------------------------------

  /**
   * Worker for {@link #prettyPrint()}.
   * 
   * @param linePrefix
   *          Prefix string containing the super-tree structure
   * @return A list of table rows, each containing three columns: tree
   *        structure, element UID, and element name
   */
  private List<String[]> int_prettyPrint(String linePrefix)
  {
    // Print this node
    String pfx  = linePrefix+"Bank Tree";
    String UID  = this.UID !=null ? this.UID  : "";
    String name = this.name!=null ? this.name : "";

    if (ROOT_UID.equals(this.UID))
      UID=" ";

    // Make print columns
    List<String[]> cols = new ArrayList<String[]>();
    cols.add(new String[]{pfx,UID,name});

    // Print tree children
    List<BankTree<T>> trees = getTrees();
    for (int i=0; i<trees.size(); i++)
    {
      String lpfx = linePrefix.replace("'-"," ")
                  + (i<trees.size()-1 ? "|- " : "'- ");
      cols.addAll(trees.get(i).int_prettyPrint(lpfx));
    }

    // Print bank children
    List<Bank> banks = getBanks(false);
    for (int i=0; i<banks.size(); i++)
    {
      String lpfx = linePrefix.replace("'-"," ")
                  + (i<banks.size()-1 ? "|- " : "'- ");
      cols.addAll(banks.get(i).int_prettyPrint(lpfx));
    }

    // Return printed columns
    return cols;
  }

  /**
   * Pretty-prints a table; worker for {@link #prettyPrint()}.
   * 
   * @param title
   *          The title of the table
   * @param rows
   *          A list of table rows, each containing three columns: tree
   *          structure, element UID, and element name
   * @return A string containing the pretty-printed table
   */
  private String int_prettyPrintTable(String title, List<String[]> rows)
  {
    if (title==null)
      title = "";
    String UID  = "UID";
    String name = "Name";

    // Pre-flight: Determines column widths
    int[] w = new int[3];
    for (int i=0; i<rows.size(); i++)
      for (int j=0; j<3; j++)
        w[j] = Math.max(w[j],rows.get(i)[j].length());

    // Print table
    // - Header
    w[0] = Math.max(w[0],title.length());
    w[1] = Math.max(w[1],UID  .length());
    w[2] = Math.max(w[2],name .length());
    String hr = "-".repeat(w[0]+w[1]+w[2]+4);
    String s  = hr+"\n";
    s += String.format("%-"+w[0]+"s   ",title);
    s += String.format("%-"+w[1]+"s "  ,UID  );
    s += String.format("%-"+w[0]+"s\n" ,name );
    s += hr+"\n";

    // - Rows
    for (int i=0; i<rows.size(); i++)
    {
      s += String.format("%-"+w[0]+"s : ",rows.get(i)[0]);
      s += String.format("%-"+w[1]+"s "  ,rows.get(i)[1]);
      s += String.format("%s\n"          ,rows.get(i)[2]);
    }

    // Footer
    s += hr+"\n";
    s += "* "+this.objectType+" data available\n";

    // Return printed table
    return s;
  }

}

// EOF