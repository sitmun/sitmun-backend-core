import React from "react";
import {Table} from "react-bootstrap";

export function Tree({nodes, permissions}) {
  const map = new Map()
  nodes.map(node => map.set(node.id, node))

  const cartographies = new Map()
  permissions.forEach(permission => {
    permission.members.forEach(cartography => {
      cartographies.set(cartography.id, cartography.name)
    })
  })

  console.log(cartographies)

  function assemble(arr, parentId = null, result = []) {
    arr.forEach(el => {
      if (el.parentId === parentId) {
        result.push(el);
        assemble(arr, el.id, result);
      }
    })
    return result;
  }

  const results = assemble(nodes);

  return (
    <Table>
      <thead>
      <tr>
        <th>Id</th>
        <th>Name</th>
        <th>Parent</th>
        <th>Cartography</th>
      </tr>
      </thead>
      <tbody>
      {results.map(node =>
        <tr key={node.id}>
          <td><code>{node.id}</code></td>
          <td><code>{node.name}</code></td>
          <td><code>{map.get(node.parentId)?.name}</code></td>
          <td><code>{cartographies.get(node.cartographyId)}</code></td>
        </tr>
      )}
      </tbody>
    </Table>
  );
}