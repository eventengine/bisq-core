/*
 * This file is part of bisq.
 *
 * bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package io.bisq.core.dao.node.consensus;

import io.bisq.core.dao.DaoOptionKeys;
import io.bisq.core.dao.blockchain.ReadableBsqBlockChain;
import io.bisq.core.dao.blockchain.WritableBsqBlockChain;
import io.bisq.core.dao.blockchain.vo.Tx;
import io.bisq.core.dao.blockchain.vo.TxOutput;
import io.bisq.core.dao.blockchain.vo.TxType;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Verifies if a given transaction is a BSQ genesis transaction.
 */
public class GenesisTxController {

    private final WritableBsqBlockChain writableBsqBlockChain;
    private final ReadableBsqBlockChain readableBsqBlockChain;
    private final String genesisTxId;
    private final int genesisBlockHeight;

    @Inject
    public GenesisTxController(WritableBsqBlockChain writableBsqBlockChain,
                               ReadableBsqBlockChain readableBsqBlockChain,
                               @Named(DaoOptionKeys.GENESIS_TX_ID) String genesisTxId,
                               @Named(DaoOptionKeys.GENESIS_BLOCK_HEIGHT) int genesisBlockHeight) {
        this.writableBsqBlockChain = writableBsqBlockChain;
        this.readableBsqBlockChain = readableBsqBlockChain;
        this.genesisTxId = genesisTxId;
        this.genesisBlockHeight = genesisBlockHeight;
    }

    public boolean isGenesisTx(Tx tx, int blockHeight) {
        return tx.getId().equals(genesisTxId) && blockHeight == genesisBlockHeight;
    }

    // TODO Refactor to new GenesisTxOutputsController class to separate verification logic
    // from state change
    public void applyStateChange(Tx tx) {
        long totalOutput = 0;
        for (int i = 0; i < tx.getOutputs().size(); ++i) {
            TxOutput txOutput = tx.getOutputs().get(i);
            totalOutput += txOutput.getValue();
            // Handle change from genesis transaction, only count outputs below the issuance amount as BSQ outputs
            if (totalOutput <= readableBsqBlockChain.getIssuedAmount().getValue()) {
                txOutput.setUnspent(true);
                txOutput.setVerified(true);
                writableBsqBlockChain.addUnspentTxOutput(txOutput);
            }
        }
        tx.setTxType(TxType.GENESIS);

        writableBsqBlockChain.setGenesisTx(tx);
        writableBsqBlockChain.addTxToMap(tx);
    }
}
