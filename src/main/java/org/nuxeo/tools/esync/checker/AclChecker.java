/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Benoit Delbosc
 */
package org.nuxeo.tools.esync.checker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.nuxeo.tools.esync.db.Document;
import org.nuxeo.tools.esync.db.Node;
import org.nuxeo.tools.esync.event.DiffEvent;
import org.nuxeo.tools.esync.event.MissingEvent;

public class AclChecker extends AbstractChecker {

    private static final Logger log = LoggerFactory.getLogger(AclChecker.class);

    @Override
    void check() {
        List<Document> docsWithAcl = db.getDocumentWithAcl();
        int aclDocumentCount = docsWithAcl.size();
        postMessage(String.format("%d documents hold an ACL", aclDocumentCount));
        compareWithEs(docsWithAcl);
        Node root = buildTree(docsWithAcl);
        if (log.isTraceEnabled()) {
            // printTree(root, 0);
        }
        checkAclConsistencyRecursive(root);
    }

    @Override
    String getName() {
        return "AclChecker";
    }

    private void checkAclConsistencyRecursive(Node root) {
        Queue<Node> queue = new LinkedList<>();
        queue.add(root);
        do {
            Node node = queue.poll();
            checkAclConsistency(node);
            queue.addAll(node.children);
        } while (!queue.isEmpty());
    }

    private void checkAclConsistency(Node node) {
        if (node.isRoot()) {
            return;
        }
        String path = node.doc.path;
        if (path == null) {
            return;
        }
        Set<String> acl = node.doc.acl;
        List<String> excludePath = new ArrayList<>();
        for (Node child : node.children) {
            excludePath.add(child.doc.path);
        }
        List<Document> invalidDocs = es.getDocsWithInvalidAcl(acl, path,
                excludePath);
        for (Document esDoc : invalidDocs) {
            Document dbDoc = db.getDocument(esDoc.id);
            // double check
            if (! esDoc.equals(dbDoc)) {
                post(new DiffEvent(dbDoc, esDoc, "Invalid ACL found"));
            }
        }
    }

    private Node buildTree(List<Document> documents) {
        sortDocumentsByPath(documents);
        List<Node> nodes = new ArrayList<>();
        Node root = new Node(null);
        for (Document doc : documents) {
            Node parent = null;
            Node node = new Node(doc);
            String path = doc.path;
            // a parent of a document is on the top of the list
            for (Node potentialAncestor : nodes) {
                String ancestorPath = potentialAncestor.doc.path;
                if (ancestorPath == null) {
                    continue;
                }
                if (path != null && path.startsWith(ancestorPath)) {
                    // the one with the longest path is the direct parent
                    if (parent == null
                            || ancestorPath.length() > parent.doc.path.length()) {
                        parent = potentialAncestor;
                    }
                }
            }
            if (parent == null) {
                root.addChildren(node);
            } else {
                parent.addChildren(node);
            }
            nodes.add(node);
        }
        return root;
    }

    private void sortDocumentsByPath(List<Document> docsWithAcl) {
        Collections.sort(docsWithAcl, new Comparator<Document>() {
            @Override
            public int compare(Document a, Document b) {
                if (a.path == b.path) {
                    return 0;
                }
                if (a.path == null) {
                    return -1;
                }
                return a.path.compareTo(b.path == null ? "NULL" : b.path);
            }
        });
    }

    private void compareWithEs(List<Document> documents) {
        Document esDoc;
        for (Document doc : documents) {
            if ("Root".equals(doc.primaryType)) {
                // root document is not present on ES
                continue;
            }
            log.debug(doc.toString());
            try {
                esDoc = es.getDocument(doc.id);
            } catch (NoSuchElementException e) {
                post(new MissingEvent(doc.id, "not found in es, " + doc));
                continue;
            }
            if (!doc.equals(esDoc)) {
                post(new DiffEvent(doc, esDoc, "ACL diff found"));
            }
            doc.merge(esDoc);
        }
    }

    private void printTree(Node parent, int depth) {
        if (parent.isRoot()) {
            System.out.println("ROOT");
        } else {
            for (int i = 0; i < depth; i++) {
                System.out.print("  ");
            }
            System.out.println(parent.doc.path + " " + parent.doc);
        }
        depth++;
        for (Node child : parent.children) {
            printTree(child, depth);
        }
    }
}
