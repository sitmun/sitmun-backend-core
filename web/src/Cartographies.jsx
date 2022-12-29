import {Table} from "react-bootstrap";
import React from "react";

export function Cartographies({permissions}) {
  const cartographies = new Map()
  permissions.forEach(permission => {
    permission.members.map(cartography => {
      if (cartographies.has(cartography.id)) {
        cartographies.get(cartography.id).push(permission.name)
      } else {
        cartographies.set(cartography.id, [cartography, permission.name])
      }
    })
  })
  const result = []
  cartographies.forEach((value, key) => {
    result.push({cartography: value.shift(), permissions: value})
  })
  result.sort((a, b) => a.cartography.id - b.cartography.id)

  return (
    <Table>
      <thead>
      <tr>
        <th>Id</th>
        <th>Name</th>
        <th>Permissions</th>
      </tr>
      </thead>
      <tbody>
      {
        result.map(value =>
          <tr key={value.cartography.id}>
            <td><code>{value.cartography.id}</code></td>
            <td><code>{value.cartography.name}</code></td>
            <td><code>{value.permissions.join(', ')}</code></td>
          </tr>
        )
      }
      </tbody>
    </Table>
  );
}